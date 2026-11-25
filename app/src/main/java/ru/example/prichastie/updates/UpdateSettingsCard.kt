package ru.example.prichastie.updates

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException
import ru.example.prichastie.ui.AppCard
import ru.example.prichastie.ui.Note
import ru.example.prichastie.ui.Sans
import ru.example.prichastie.ui.theme.palette

private fun mb(bytes: Long) = String.format(Locale.forLanguageTag("ru"), "%.1f", bytes / 1_048_576.0)

@Composable
fun UpdateSettingsCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val p = palette
    @Suppress("DEPRECATION")
    val installed = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf<ReleaseUpdate?>(null) }
    var openedSettings by remember { mutableStateOf(false) }
    val download by ApkUpdater.state.collectAsState()

    // Автоустановка: как только APK скачан и приложение на экране.
    LaunchedEffect(download) {
        val s = download
        if (s is DownloadState.Downloaded && s.autoStart) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                when (val r = ApkInstaller.install(context, s.file)) {
                    InstallResult.Started -> ApkUpdater.installStarted(s.file)
                    InstallResult.NeedsPermission -> ApkUpdater.needsPermission(s.file)
                    is InstallResult.Error -> ApkUpdater.fail(r.text)
                }
            }
        }
    }

    // Возврат из настроек «Установка неизвестных приложений».
    LifecycleResumeEffect(download, openedSettings) {
        val s = download
        if (openedSettings && s is DownloadState.NeedsPermission) {
            if (context.packageManager.canRequestPackageInstalls()) {
                openedSettings = false
                ApkUpdater.retryInstall(s.file)
            }
        }
        onPauseOrDispose { }
    }

    AppCard {
        Text("Обновления", color = p.ink, fontFamily = Sans, fontWeight = FontWeight.Bold)
        Note("Установленная версия: $installed")

        when (val s = download) {
            is DownloadState.Downloading -> {
                if (s.total > 0) {
                    val fraction = (s.loaded.toFloat() / s.total).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { fraction },
                        color = p.ink,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Note("Скачано ${mb(s.loaded)} из ${mb(s.total)} МБ (${(fraction * 100).toInt()}%)")
                } else {
                    LinearProgressIndicator(color = p.ink, modifier = Modifier.fillMaxWidth())
                    Note("Скачано ${mb(s.loaded)} МБ")
                }
                TextButton(onClick = { ApkUpdater.cancel() }) { Text("Отменить", color = p.ink) }
            }
            is DownloadState.Downloaded -> {
                Note(
                    if (s.autoStart) "Обновление скачано. Открываю установщик…"
                    else "Обновление скачано. Если установка не началась или была отменена, нажмите кнопку."
                )
                if (!s.autoStart) {
                    TextButton(onClick = { ApkUpdater.retryInstall(s.file) }) {
                        Text("Установить", color = p.ink)
                    }
                }
            }
            is DownloadState.Failed -> {
                Note(s.text)
                TextButton(onClick = { ApkUpdater.reset() }) { Text("Закрыть", color = p.ink) }
            }
            else -> Unit
        }

        TextButton(
            enabled = !checking && download !is DownloadState.Downloading,
            onClick = {
                checking = true
                message = null
                available = null
                ApkUpdater.reset()
                scope.launch {
                    try {
                        when (val result = GitHubReleaseChecker.check(installed)) {
                            is UpdateCheck.Available -> available = result.release
                            is UpdateCheck.Message -> message = result.text
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: IOException) {
                        message = "Не удалось связаться с GitHub. Проверьте интернет и повторите попытку."
                    } catch (_: JSONException) {
                        message = "GitHub вернул неожиданный ответ. Попробуйте позже."
                    } catch (_: SecurityException) {
                        message = "Нет доступа к интернету. Проверьте разрешения приложения."
                    } finally {
                        checking = false
                    }
                }
            }
        ) {
            Text(if (checking) "Проверка…" else "Проверить обновления", color = p.ink)
        }
        message?.let { Note(it) }
        Note("Проверка только по нажатию. APK скачивается внутри приложения, после загрузки откроется установщик Android.")
    }

    available?.let { release ->
        AlertDialog(
            onDismissRequest = { available = null },
            title = { Text("Доступна версия ${release.tag}") },
            text = {
                Column(
                    Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (release.notes.isNotBlank()) Text(release.notes)
                    if (release.apkUrl == null) {
                        Text("В релизе нет единственного готового APK. Автор должен приложить один универсальный подписанный APK.")
                    } else {
                        Text("APK скачается в приложении, прогресс будет виден в настройках. После загрузки откроется установщик Android; при первом запуске может понадобиться разрешить установку из этого приложения.")
                    }
                }
            },
            confirmButton = {
                if (release.apkUrl != null) {
                    TextButton(onClick = {
                        available = null
                        if (!GitHubReleaseChecker.isTrustedApkUrl(release.apkUrl)) {
                            message = "Неверная ссылка на APK. Скачивание отменено."
                        } else {
                            message = null
                            ApkUpdater.start(context, release)
                        }
                    }) { Text("Скачать и установить") }
                } else {
                    TextButton(onClick = { available = null }) { Text("Понятно") }
                }
            },
            dismissButton = {
                if (release.apkUrl != null) {
                    TextButton(onClick = { available = null }) { Text("Позже") }
                }
            }
        )
    }

    (download as? DownloadState.NeedsPermission)?.let {
        AlertDialog(
            onDismissRequest = { ApkUpdater.reset() },
            title = { Text("Нужно разрешение") },
            text = {
                Text("Чтобы установить обновление, разрешите приложению устанавливать программы. Откроются настройки: включите переключатель и вернитесь назад, установка продолжится сама.")
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        openedSettings = true
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                "package:${context.packageName}".toUri()
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: ActivityNotFoundException) {
                        openedSettings = false
                        ApkUpdater.fail("Не удалось открыть настройки. Разрешите установку вручную в настройках приложения.")
                    }
                }) { Text("Открыть настройки") }
            },
            dismissButton = {
                TextButton(onClick = { ApkUpdater.reset() }) { Text("Отмена") }
            }
        )
    }
}