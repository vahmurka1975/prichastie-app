package ru.example.prichastie.updates

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal sealed interface DownloadState {
    data object Idle : DownloadState
    /** total = -1, если сервер не сообщил размер. */
    data class Downloading(val loaded: Long, val total: Long) : DownloadState
    /** autoStart = true — установщик ещё не открывали. */
    data class Downloaded(val file: File, val autoStart: Boolean) : DownloadState
    data class NeedsPermission(val file: File) : DownloadState
    data class Failed(val text: String) : DownloadState
}

internal sealed interface InstallResult {
    data object Started : InstallResult
    data object NeedsPermission : InstallResult
    data class Error(val text: String) : InstallResult
}

internal object ApkUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state
    private var job: Job? = null

    fun start(context: Context, release: ReleaseUpdate) {
        val url = release.apkUrl ?: return
        if (job?.isActive == true) return
        val app = context.applicationContext
        _state.value = DownloadState.Downloading(0, -1)
        job = scope.launch {
            val dir = File(app.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val part = File(dir, "update.apk.part")
            val target = File(dir, "update.apk")
            try {
                download(url, part)
                validate(app, part)?.let { throw IOException(it) }
                if (!part.renameTo(target)) throw IOException("Не удалось сохранить файл обновления.")
                _state.value = DownloadState.Downloaded(target, autoStart = true)
            } catch (c: CancellationException) {
                part.delete()
                _state.value = DownloadState.Idle
                throw c
            } catch (e: IOException) {
                part.delete()
                _state.value = DownloadState.Failed(
                    e.message?.takeIf { it.isNotBlank() } ?: "Не удалось скачать обновление."
                )
            } catch (e: SecurityException) {
                part.delete()
                _state.value = DownloadState.Failed("Нет доступа к интернету или памяти.")
            }
        }
    }

    fun cancel() { job?.cancel() }
    fun reset() { if (job?.isActive != true) _state.value = DownloadState.Idle }
    fun installStarted(file: File) { _state.value = DownloadState.Downloaded(file, autoStart = false) }
    fun needsPermission(file: File) { _state.value = DownloadState.NeedsPermission(file) }
    fun retryInstall(file: File) { _state.value = DownloadState.Downloaded(file, autoStart = true) }
    fun fail(text: String) { _state.value = DownloadState.Failed(text) }

    // ---------- скачивание ----------

    private fun isAllowedHost(url: URL) =
        url.protocol == "https" &&
            (url.host == "github.com" || url.host.endsWith(".githubusercontent.com"))

    private fun open(start: String): HttpURLConnection {
        var current = start
        repeat(6) {
            val url = URL(current)
            if (!isAllowedHost(url)) throw IOException("Недоверенный адрес скачивания.")
            val c = url.openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.instanceFollowRedirects = false
            c.useCaches = false
            c.setRequestProperty("User-Agent", "Prichastie-Android-UpdateChecker")
            c.setRequestProperty("Accept-Encoding", "identity") // чтобы Content-Length был точным
            when (val code = c.responseCode) {
                200 -> return c
                301, 302, 303, 307, 308 -> {
                    val location = c.getHeaderField("Location")
                    c.disconnect()
                    if (location.isNullOrBlank()) throw IOException("Пустое перенаправление.")
                    current = URL(url, location).toString()
                }
                else -> {
                    c.disconnect()
                    throw IOException("Сервер вернул ошибку HTTP $code.")
                }
            }
        }
        throw IOException("Слишком много перенаправлений.")
    }

    private fun CoroutineScope.download(url: String, out: File) {
        val c = open(url)
        try {
            val total = c.contentLengthLong
            var loaded = 0L
            var lastEmit = 0L
            _state.value = DownloadState.Downloading(0, total)
            c.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (!isActive) throw CancellationException()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        loaded += n
                        val now = System.nanoTime()
                        if (now - lastEmit > 150_000_000L) {
                            lastEmit = now
                            _state.value = DownloadState.Downloading(loaded, total)
                        }
                    }
                }
            }
            if (total > 0 && loaded != total) throw IOException("Файл скачан не полностью. Повторите попытку.")
            _state.value = DownloadState.Downloading(loaded, if (total > 0) total else loaded)
        } finally {
            c.disconnect()
        }
    }

    // ---------- проверка APK ----------

    @Suppress("DEPRECATION")
    private fun signatures(pm: PackageManager, info: android.content.pm.PackageInfo): Set<Signature> =
        if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.toSet().orEmpty()
        } else {
            info.signatures?.toSet().orEmpty()
        }

    @Suppress("DEPRECATION")
    private fun validate(context: Context, apk: File): String? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
        else PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: return "Скачанный файл не является корректным APK."
        if (archive.packageName != context.packageName) return "Это APK другого приложения."
        val installed = pm.getPackageInfo(context.packageName, flags)
        if (PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)) {
            return "В релизе не новее установленной версии (versionCode). Увеличьте versionCode и опубликуйте релиз заново."
        }
        val a = signatures(pm, archive)
        val b = signatures(pm, installed)
        if (a.isNotEmpty() && b.isNotEmpty() && a != b) {
            return "Подпись APK не совпадает с установленным приложением. Обновление поверх невозможно."
        }
        return null
    }
}

internal object ApkInstaller {
    fun install(context: Context, file: File): InstallResult {
        if (!file.exists()) return InstallResult.Error("Файл обновления не найден. Скачайте его заново.")
        if (!context.packageManager.canRequestPackageInstalls()) return InstallResult.NeedsPermission
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            InstallResult.Started
        } catch (_: ActivityNotFoundException) {
            InstallResult.Error("На устройстве нет установщика пакетов.")
        } catch (_: SecurityException) {
            InstallResult.Error("Android запретил запуск установки.")
        } catch (_: IllegalArgumentException) {
            InstallResult.Error("Не удалось выдать доступ к файлу обновления.")
        }
    }
}