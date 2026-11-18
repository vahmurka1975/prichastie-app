package ru.example.prichastie

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.example.prichastie.ui.AppBar
import ru.example.prichastie.ui.AppButton
import ru.example.prichastie.ui.AppCard
import ru.example.prichastie.ui.HLine
import ru.example.prichastie.ui.Note
import ru.example.prichastie.ui.SegToggle
import ru.example.prichastie.ui.Sans
import ru.example.prichastie.ui.Serif
import ru.example.prichastie.ui.theme.palette
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    ui: UiState,
    settings: AppSettings,
    scroll: ScrollState,
    onSetFullMode: (Boolean) -> Unit,
    onFontSize: (Int) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onRestartAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = palette
    var draft by remember(settings.fontSizeSp) { mutableFloatStateOf(settings.fontSizeSp.toFloat()) }
    val themes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)

    Column(modifier.fillMaxSize()) {
        AppBar("Настройки")
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard {
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = !ui.started) { onSetFullMode(!ui.fullMode) },
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(checked = ui.fullMode, onCheckedChange = null, enabled = !ui.started)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Полное прохождение", color = p.ink, fontFamily = Sans,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Note(
                            "Включает три канона (№1–3) в прогресс. " +
                                if (ui.started) "🔒 Подготовка начата, менять нельзя: сначала «Начать всё сначала»."
                                else "Отметьте только в самом начале подготовки."
                        )
                    }
                }
                Spacer(Modifier.padding(top = 10.dp))
                HLine()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Тема", color = p.ink, fontFamily = Sans, fontSize = 14.sp)
                    SegToggle(
                        listOf("День", "Ночь", "Авто"),
                        themes.indexOf(settings.themeMode).coerceAtLeast(0),
                        { onTheme(themes[it]) },
                        Modifier.width(200.dp)
                    )
                }
                HLine()
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Размер текста", color = p.ink, fontFamily = Sans, fontSize = 14.sp)
                    Text("${draft.roundToInt()}", color = p.mut, fontFamily = Sans, fontSize = 14.sp)
                }
                Slider(
                    value = draft,
                    onValueChange = { draft = it },
                    onValueChangeFinished = { onFontSize(draft.roundToInt()) },
                    valueRange = MIN_FONT_SP.toFloat()..MAX_FONT_SP.toFloat(),
                    steps = MAX_FONT_SP - MIN_FONT_SP - 1
                )
                Text(
                    "Господи, помилуй", color = p.ink, fontFamily = Serif,
                    fontSize = draft.sp, lineHeight = (draft * 1.4f).sp
                )
            }

            AppCard {
                Note(
                    "Шаг засчитывается кнопкой «Прочитано» в конце текста: она становится активной, " +
                        "когда текст дочитан до конца. Само Причащение отмечается кнопкой «Готово». " +
                        "Язык текста можно менять в любой момент."
                )
            }

            AppButton("↺ Начать всё сначала", onRestartAll, outlined = true)
        }
    }
}