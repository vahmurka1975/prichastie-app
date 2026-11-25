package ru.example.prichastie.updates

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class ReleaseUpdate(val tag: String, val notes: String, val apkUrl: String?)
internal sealed class UpdateCheck {
    data class Available(val release: ReleaseUpdate) : UpdateCheck()
    data class Message(val text: String) : UpdateCheck()
}
internal object GitHubReleaseChecker {
    private const val API = "https://api.github.com/repos/vahmurka1975/prichastie-app/releases/latest"
    private const val DOWNLOAD_PATH = "/vahmurka1975/prichastie-app/releases/download/"
    suspend fun check(installedVersion: String): UpdateCheck = withContext(Dispatchers.IO) {
        val connection = URL(API).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "Prichastie-Android-UpdateChecker")
            when (connection.responseCode) {
                404 -> return@withContext UpdateCheck.Message("Опубликованных стабильных релизов пока нет.")
                403, 429 -> return@withContext UpdateCheck.Message("GitHub временно ограничил запросы. Попробуйте позже.")
                200 -> Unit
                else -> throw IOException("GitHub HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(8192)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    if (result.length + count > 2_000_000) throw IOException("Release response too large")
                    result.append(buffer, 0, count)
                }
                result.toString()
            }
            val json = JSONObject(body)
            if (json.optBoolean("draft") || json.optBoolean("prerelease")) {
                return@withContext UpdateCheck.Message("Стабильный релиз недоступен.")
            }
            val tag = json.getString("tag_name")
            when (ReleaseVersion.isNewer(tag, installedVersion)) {
                null -> return@withContext UpdateCheck.Message("Не удалось сравнить версии. Нужен формат v1.2.3; установлено: $installedVersion, релиз: $tag.")
                false -> return@withContext UpdateCheck.Message("Обновлений нет. Установлена версия $installedVersion; последний стабильный релиз — $tag.")
                true -> Unit
            }
            val urls = mutableListOf<String>()
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("state") != "uploaded") continue
                    if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
                    val url = asset.optString("browser_download_url")
                    if (isTrustedApkUrl(url)) urls.add(url)
                }
            }
            // Require one universal APK; never guess between ABI/debug artifacts.
            UpdateCheck.Available(ReleaseUpdate(tag, json.optString("body").take(6000), urls.singleOrNull()))
        } finally {
            connection.disconnect()
        }
    }
    fun isTrustedApkUrl(value: String): Boolean = try {
        val url = URL(value)
        url.protocol == "https" && url.host == "github.com" && url.port == -1 &&
            url.userInfo == null && url.path.startsWith(DOWNLOAD_PATH) &&
            url.path.endsWith(".apk", ignoreCase = true) && url.query == null && url.ref == null
    } catch (_: Exception) {
        false
    }
}
