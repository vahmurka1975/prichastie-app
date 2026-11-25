package ru.example.prichastie.updates

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONException
import ru.example.prichastie.ui.AppCard
import ru.example.prichastie.ui.Note
import ru.example.prichastie.ui.Sans
import ru.example.prichastie.ui.theme.palette

@Composable
fun UpdateSettingsCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val p = palette
    @Suppress("DEPRECATION")
    val installed = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf<ReleaseUpdate?>(null) }
    AppCard {
        Text("Обновления", color = p.ink, fontFamily = Sans, fontWeight = FontWeight.Bold)
        Note("Установленная версия: $installed")
        TextButton(enabled = !checking, onClick = {
            checking = true
            message = null
            available = null
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
        }) {
            Text(if (checking) "Проверка…" else "Проверить обновления", color = p.ink)
        }
        message?.let { Note(it) }
        Note("Проверка только по нажатию. APK скачивается через браузер; установку подтверждаете вы.")
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
                        Text("Откроется браузер для скачивания APK из GitHub Releases. После загрузки откройте файл и подтвердите установку. Android может запросить разрешение на установку из этого источника.")
                    }
                }
            },
            confirmButton = {
                if (release.apkUrl != null) {
                    TextButton(onClick = {
                        if (!GitHubReleaseChecker.isTrustedApkUrl(release.apkUrl)) {
                            message = "Неверная ссылка на APK. Скачивание отменено."
                            available = null
                        } else {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, release.apkUrl.toUri())
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                                available = null
                                message = "Ссылка на APK открыта. После скачивания откройте файл для установки."
                            } catch (_: ActivityNotFoundException) {
                                available = null
                                message = "Не найден браузер для скачивания APK."
                            } catch (_: SecurityException) {
                                available = null
                                message = "Android запретил открытие ссылки."
                            }
                        }
                    }) { Text("Скачать APK") }
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
}
