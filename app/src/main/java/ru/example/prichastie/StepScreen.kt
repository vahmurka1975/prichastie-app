package ru.example.prichastie

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.example.prichastie.model.Language
import ru.example.prichastie.model.StepKind
import ru.example.prichastie.audio.AudioBar

@Composable
fun StepScreen(
    stepId: String,
    fontSizeSp: Int,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onOpenStep: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: MainViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // Пока DataStore не прочитан, список пуст: ждём, а не закрываем экран.
    if (ui.steps.isEmpty()) return

    val included = ui.steps.filter { it.included }
    val index = included.indexOfFirst { it.step.id == stepId }
    if (index < 0) {
        // Шаг не входит в выбранный режим (например, канон при сокращённом прохождении).
        LaunchedEffect(stepId) { onClose() }
        return
    }

    val cur = included[index]
    val step = cur.step
    val isLast = index == included.lastIndex

    var language by rememberSaveable { mutableStateOf(Language.MODERN) }

    Column(modifier = modifier.fillMaxSize()) {

        // Верхняя строка: назад к списку, настройки и номер шага
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) { Text("← Список") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenSettings) { Text("Aa") }
            Text(
                text = "${cur.number} из ${included.size}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / included.size },
            modifier = Modifier.fillMaxWidth()
        )

        // Заголовок
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(step.title, style = MaterialTheme.typography.titleLarge)
            if (step.subtitle.isNotBlank()) {
                Text(
                    step.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (step.kind == StepKind.COMMUNION) {
            // Для Причастия чтения нет: пользователь только подтверждает.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    "Здесь чтения нет. Нажмите «Далее», когда Причащение совершилось.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            // Переключатель языка
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = language == Language.MODERN,
                    onClick = { language = Language.MODERN },
                    label = { Text("Современный") }
                )
                FilterChip(
                    selected = language == Language.CHURCH_SLAVONIC,
                    onClick = { language = Language.CHURCH_SLAVONIC },
                    label = { Text("Церковнославянский") }
                )
            }

            // key(stepId): при переходе к другому шагу прокрутка начинается сверху
            key(stepId) {
                StepText(
                    textKey = step.textKey,
                    language = language,
                    fontSizeSp = fontSizeSp,
                    modifier = Modifier.weight(1f)
                )
            }

            // ▼▼▼ НОВОЕ: аудиопанель под текстом ▼▼▼
            key(stepId) {
                AudioBar(stepId = step.id, title = step.title)
            }
            // ▲▲▲ конец вставки ▲▲▲
        }

        // Нижние кнопки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onOpenStep(included[index - 1].step.id) },
                enabled = index > 0,
                modifier = Modifier.weight(1f)
            ) { Text("Назад") }

            Button(
                onClick = {
                    vm.setDone(step.id, true)
                    if (isLast) onClose() else onOpenStep(included[index + 1].step.id)
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (isLast) "Завершить" else "Далее") }
        }
    }
}

@Composable
private fun StepText(
    textKey: String,
    language: Language,
    fontSizeSp: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val text by produceState(initialValue = "", textKey, language) {
        value = withContext(Dispatchers.IO) { TextLoader.load(context, textKey, language) }
    }
    Text(
        text = text,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 1.5f).sp,
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    )
}