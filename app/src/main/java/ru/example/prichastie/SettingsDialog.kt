package ru.example.prichastie

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    fontSizeSp: Int,
    themeMode: ThemeMode,
    onFontSizeChange: (Int) -> Unit,   // вызывается, когда ползунок отпущен
    onThemeChange: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    // Пока ползунок двигают, меняется только пример текста в окне.
    var draft by remember { mutableFloatStateOf(fontSizeSp.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Размер текста: ${draft.roundToInt()}")
                Slider(
                    value = draft,
                    onValueChange = { draft = it },
                    onValueChangeFinished = { onFontSizeChange(draft.roundToInt()) },
                    valueRange = MIN_FONT_SP.toFloat()..MAX_FONT_SP.toFloat(),
                    steps = MAX_FONT_SP - MIN_FONT_SP - 1
                )
                Text(
                    "Господи, помилуй",
                    fontSize = draft.sp,
                    lineHeight = (draft * 1.4f).sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Тема")
                val options = listOf(
                    ThemeMode.SYSTEM to "Как в системе",
                    ThemeMode.LIGHT to "Светлая",
                    ThemeMode.DARK to "Тёмная"
                )
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == mode, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        }
    )
}