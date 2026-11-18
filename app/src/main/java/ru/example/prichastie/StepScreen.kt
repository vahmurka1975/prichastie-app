package ru.example.prichastie

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.example.prichastie.audio.AudioBar
import ru.example.prichastie.model.Language
import ru.example.prichastie.model.Phase
import ru.example.prichastie.model.StepKind
import ru.example.prichastie.ui.AppBar
import ru.example.prichastie.ui.AppButton
import ru.example.prichastie.ui.AppCard
import ru.example.prichastie.ui.CircleButton
import ru.example.prichastie.ui.HLine
import ru.example.prichastie.ui.Note
import ru.example.prichastie.ui.PBar
import ru.example.prichastie.ui.SegToggle
import ru.example.prichastie.ui.Sans
import ru.example.prichastie.ui.Serif
import ru.example.prichastie.ui.theme.palette

@Composable
fun StepScreen(
    stepId: String,
    fontSizeSp: Int,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onOpenStep: (String) -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier,
    vm: MainViewModel = viewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // Пока DataStore не прочитан, список пуст: ждём, а не закрываем экран.
    if (ui.steps.isEmpty()) return

    val cur = ui.steps.firstOrNull { it.included && it.step.id == stepId }
    if (cur == null) {
        // Шаг не входит в выбранный режим (например, канон при сокращённом прохождении).
        LaunchedEffect(stepId) { onClose() }
        return
    }

    if (cur.step.kind == StepKind.COMMUNION) {
        CommunionView(ui, cur, vm, onClose, onOpenStep, modifier)
    } else {
        ReadingView(ui, cur, fontSizeSp, vm, onOpenSettings, onClose, onOpenStep, onOpenSummary, modifier)
    }
}

@Composable
private fun CommunionView(
    ui: UiState,
    cur: StepUi,
    vm: MainViewModel,
    onClose: () -> Unit,
    onOpenStep: (String) -> Unit,
    modifier: Modifier
) {
    val p = palette
    val pct = ui.phaseSteps(Phase.BEFORE).percent()
    val firstAfter = ui.phaseSteps(Phase.AFTER).firstOrNull()

    Column(modifier.fillMaxSize()) {
        AppBar(cur.step.title, "между подготовкой и благодарением", onBack = onClose)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard {
                Text(
                    "«Тело Христово приимите, Источника безсмертнаго вкусите»",
                    color = p.ink, fontFamily = Serif, fontSize = 18.sp, lineHeight = 25.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Note(
                    "Таинство совершается в храме. Это шаг-отметка: он делит путь на «до» и «после».",
                    Modifier.fillMaxWidth(), TextAlign.Center
                )
            }
            AppCard {
                Text("Подготовка завершена на $pct%", color = p.mut, fontFamily = Sans, fontSize = 13.sp)
                PBar(pct / 100f, modifier = Modifier.padding(vertical = 8.dp))
                if (pct < 100) Note("Некоторые шаги не пройдены, но отметку можно поставить: решение за вами.")
            }
            if (cur.done) {
                if (firstAfter != null) {
                    AppButton("К благодарственным молитвам", { onOpenStep(firstAfter.step.id) })
                }
                AppButton("Снять отметку", { vm.setDone(cur.step.id, false) }, outlined = true)
            } else {
                AppButton("Готово: я причастился(ась)", { vm.setDone(cur.step.id, true) })
            }
        }
    }
}

@Composable
private fun ReadingView(
    ui: UiState,
    cur: StepUi,
    fontSizeSp: Int,
    vm: MainViewModel,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onOpenStep: (String) -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier
) {
    val p = palette
    val step = cur.step
    val after = step.phase == Phase.AFTER
    val phaseList = ui.phaseSteps(step.phase)
    val idx = phaseList.indexOfFirst { it.step.id == step.id }
    val phasePct = phaseList.percent()
    val tag = if (after) "После Причастия" else "До Причастия"

    val included = ui.steps.filter { it.included }
    val pos = included.indexOfFirst { it.step.id == step.id }
    val next = included.getOrNull(pos + 1)

    var language by rememberSaveable { mutableStateOf(Language.MODERN) }
    val context = LocalContext.current
    val text by produceState<String?>(null, step.textKey, language) {
        value = withContext(Dispatchers.IO) { TextLoader.load(context, step.textKey, language) }
    }

    // У каждого шага своя прокрутка: «Прочитано» активно, когда текст дочитан до конца.
    val scroll: ScrollState = remember(step.id) { ScrollState(0) }
    val canRead = text != null &&
        (scroll.maxValue <= 24 || scroll.value >= scroll.maxValue - 24)

    Column(modifier.fillMaxSize()) {
        AppBar(
            title = step.title,
            subtitle = "$tag · шаг ${idx + 1} из ${phaseList.size} · " +
                if (step.id == "church_prayer") "отметка" else "≈ " + fmtMin(step.audioSeconds),
            onBack = onClose,
            actions = { CircleButton("Aa", onOpenSettings, fontSize = 13.sp) }
        )

        // Сегменты этапа: по одному на шаг
        Row(
            Modifier.fillMaxWidth().background(p.card).padding(start = 14.dp, end = 14.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val seg = RoundedCornerShape(2.dp)
            phaseList.forEach { s ->
                val isCur = s.step.id == step.id
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(seg)
                        .background(if (s.done) (if (after) p.ok else p.gold) else p.line)
                        .then(if (isCur) Modifier.border(2.dp, p.acc, seg) else Modifier)
                        .clickable { onOpenStep(s.step.id) }
                )
            }
        }

        // Легенда: прогресс шага и этапа
        Column(Modifier.fillMaxWidth().background(p.card).padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Этот шаг: ${if (cur.done) 100 else 0}%", color = p.mut, fontFamily = Sans, fontSize = 11.sp)
                Text("$tag: $phasePct%", color = p.mut, fontFamily = Sans, fontSize = 11.sp)
            }
            PBar(if (cur.done) 1f else 0f, height = 4.dp, color = p.acc, modifier = Modifier.padding(top = 3.dp))
            PBar(phasePct / 100f, height = 4.dp, ok = after, modifier = Modifier.padding(top = 3.dp))
        }
        HLine()

        // Язык текста
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp)) {
            SegToggle(
                listOf("Современный", "Церковнославянский"),
                if (language == Language.MODERN) 0 else 1,
                { language = if (it == 0) Language.MODERN else Language.CHURCH_SLAVONIC },
                Modifier.weight(1f)
            )
        }

        // Текст шага
        key(step.id) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(14.dp)
            ) {
                AppCard {
                    if (step.subtitle.isNotBlank()) {
                        Note(step.subtitle, Modifier.padding(bottom = 10.dp))
                    }
                    Text(
                        text ?: "",
                        color = p.ink, fontFamily = Serif,
                        fontSize = fontSizeSp.sp, lineHeight = (fontSizeSp * 1.55f).sp
                    )
                }
            }
        }

        // Аудиопанель (внешний вид AudioBar ещё не переделан под макет)
        key(step.id) {
            Box(Modifier.fillMaxWidth().background(p.card)) {
                AudioBar(stepId = step.id, title = step.title)
            }
        }

        // Нижняя панель: «Прочитано» или переход дальше
        Column(Modifier.fillMaxWidth().background(p.card)) {
            HLine()
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!cur.done) {
                    if (!canRead) {
                        Note("Дочитайте до конца: тогда кнопка станет активной",
                            Modifier.fillMaxWidth(), TextAlign.Center)
                    }
                    AppButton("Прочитано ✓", { vm.setDone(step.id, true) }, enabled = canRead)
                } else {
                    Text("Шаг пройден ✓", color = p.ok, fontFamily = Sans, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    if (step.groupId == "canons" && next?.step?.groupId == "prep") {
                        Note("Каноны окончены. Последование можно начать сейчас.",
                            Modifier.fillMaxWidth(), TextAlign.Center)
                    }
                    if (step.groupId == "prep" && next?.step?.groupId == "church") {
                        Note("Домашнее последование окончено. Дальше — шаги в храме, в день Причастия.",
                            Modifier.fillMaxWidth(), TextAlign.Center)
                    }
                    if (next != null) {
                        AppButton(
                            if (next.step.kind == StepKind.COMMUNION) "Дальше: Причащение"
                            else "Дальше: " + next.step.title,
                            { onOpenStep(next.step.id) }
                        )
                    } else {
                        Note("Чин окончен. Слава Богу за всё!", Modifier.fillMaxWidth(), TextAlign.Center)
                        AppButton("✦ Перейти к итогам", onOpenSummary)
                    }
                    AppButton("↺ Пройти заново", { vm.setDone(step.id, false) }, outlined = true)
                }
            }
        }
    }
}