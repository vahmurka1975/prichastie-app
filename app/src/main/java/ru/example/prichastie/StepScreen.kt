package ru.example.prichastie

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.example.prichastie.audio.AudioMode
import ru.example.prichastie.model.Language
import ru.example.prichastie.model.Phase
import ru.example.prichastie.model.ProgressMath
import ru.example.prichastie.model.ReadingMode
import ru.example.prichastie.model.StepKind
import ru.example.prichastie.ui.AppBar
import ru.example.prichastie.ui.AppButton
import ru.example.prichastie.ui.AppCard
import ru.example.prichastie.ui.CircleButton
import ru.example.prichastie.ui.Confirm
import ru.example.prichastie.ui.ConfirmDialog
import ru.example.prichastie.ui.HLine
import ru.example.prichastie.ui.LinkText
import ru.example.prichastie.ui.Note
import ru.example.prichastie.ui.PBar
import ru.example.prichastie.ui.SegToggle
import ru.example.prichastie.ui.Sans
import ru.example.prichastie.ui.Serif
import ru.example.prichastie.ui.theme.palette
import kotlin.math.max
import kotlin.math.roundToInt

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
        StepView(ui, cur, fontSizeSp, vm, onOpenSettings, onClose, onOpenStep, onOpenSummary, modifier)
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
private fun TextCard(subtitle: String, text: String?, fontSizeSp: Int) {
    AppCard {
        if (subtitle.isNotBlank()) Note(subtitle, Modifier.padding(bottom = 10.dp))
        Text(
            text ?: "", color = palette.ink, fontFamily = Serif,
            fontSize = fontSizeSp.sp, lineHeight = (fontSizeSp * 1.55f).sp
        )
    }
}

@Composable
private fun StepView(
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
    val pr = cur.progress
    val done = cur.done
    val marker = step.isMarker
    val after = step.phase == Phase.AFTER
    val phaseList = ui.phaseSteps(step.phase)
    val idx = phaseList.indexOfFirst { it.step.id == step.id }
    val phasePct = phaseList.percent()
    val tag = if (after) "После Причастия" else "До Причастия"

    val included = ui.steps.filter { it.included }
    val pos = included.indexOfFirst { it.step.id == step.id }
    val next = included.getOrNull(pos + 1)

    var confirm by remember { mutableStateOf<Confirm?>(null) }

    // Способ: у начатого шага он зафиксирован, иначе берём «по умолчанию»
    val locked = !done && pr.mode != null
    val mode = if (!done) pr.mode ?: ui.defaultMode else ui.defaultMode
    val audio = !done && !marker && mode == ReadingMode.AUDIO
    // У начатого аудио язык записи зафиксирован
    val effLang = if (!done && pr.mode == ReadingMode.AUDIO) pr.language ?: ui.language else ui.language

    val context = LocalContext.current
    val text by produceState<String?>(null, step.textKey, effLang, marker) {
        value = if (marker) "" else withContext(Dispatchers.IO) {
            TextLoader.load(context, step.textKey, effLang)
        }
    }

    // У каждого шага своя прокрутка
    val scroll: ScrollState = remember(step.id) { ScrollState(0) }
    val maxV = scroll.maxValue
    val ready = text != null && maxV != Int.MAX_VALUE
    val noScroll = ready && maxV <= 24
    val liveFrac = if (ready && !noScroll) ProgressMath.scrollFraction(scroll.value, maxV) else 0f
    val trackRead = !done && !marker && !audio && ready && !noScroll
    val canRead = ready && (noScroll || max(liveFrac, pr.readFraction) >= ProgressMath.END_THRESHOLD)

    val trackNow by rememberUpdatedState(trackRead)
    val liveNow by rememberUpdatedState(liveFrac)

    // Доля шага для полосок: для чтения берём живую прокрутку
    val shownFrac = when {
        done -> 1f
        trackRead -> {
            val f = max(pr.readFraction, liveFrac)
            if (pr.mode == null && f < 0.02f) 0f else f.coerceAtMost(0.99f)
        }
        else -> cur.frac
    }
    val stepPct = (shownFrac * 100).roundToInt()

    // Сохраняем прокрутку с задержкой, а при уходе с экрана досохраняем
    LaunchedEffect(step.id) {
        snapshotFlow {
            if (scroll.maxValue == Int.MAX_VALUE) -1f
            else ProgressMath.scrollFraction(scroll.value, scroll.maxValue)
        }.collectLatest { f ->
            delay(400)
            if (f >= 0f && trackNow) vm.recordRead(step.id, f)
        }
    }
    DisposableEffect(step.id) {
        onDispose { if (trackNow) vm.recordRead(step.id, liveNow) }
    }
    // Возвращаемся к месту, где остановились
    LaunchedEffect(step.id, text != null) {
        if (text != null && !done && pr.mode == ReadingMode.TEXT && pr.readFraction > 0.02f) {
            val m = snapshotFlow { scroll.maxValue }.first { it in 1 until Int.MAX_VALUE }
            scroll.scrollTo((pr.readFraction * m).roundToInt())
        }
    }

    fun modeName(m: ReadingMode?) = if (m == ReadingMode.AUDIO) "Аудио" else "Чтение"

    val restartConfirm = Confirm(
        title = "Начать шаг сначала?",
        body = "Прогресс этого шага ($stepPct%) будет сброшен. Способ (чтение или аудио) можно выбрать заново.",
        action = "Начать сначала",
        onYes = { vm.resetStep(step.id) }
    )

    val onMode: (Int) -> Unit = { i ->
        val m = if (i == 0) ReadingMode.TEXT else ReadingMode.AUDIO
        if (m != mode) {
            if (locked) {
                confirm = Confirm(
                    title = "Сменить способ?",
                    body = "Шаг начат в режиме «${modeName(mode)}» ($stepPct%). Чтение и слушание нельзя " +
                        "совмещать в одном шаге: чтобы перейти, шаг придётся начать сначала. " +
                        "Если нажать «Отмена», шаг продолжится как был.",
                    action = "Начать сначала: " + if (m == ReadingMode.AUDIO) "🎧 слушать" else "📖 читать",
                    onYes = { vm.resetStep(step.id); vm.setDefaultMode(m) }
                )
            } else {
                vm.setDefaultMode(m)
            }
        }
    }

    val onLang: (Int) -> Unit = { i ->
        val l = if (i == 0) Language.MODERN else Language.CHURCH_SLAVONIC
        if (l != effLang) {
            if (!done && pr.mode == ReadingMode.AUDIO) {
                val name = if (l == Language.MODERN) "современном русском" else "церковнославянском"
                confirm = Confirm(
                    title = "Другой язык — другая запись",
                    body = "Запись на другом языке имеет иную длительность, поэтому перенести место нельзя. " +
                        "Можно продолжить на прежнем языке («Отмена») или начать шаг сначала на $name.",
                    action = "Начать сначала на $name",
                    onYes = {
                        vm.resetStep(step.id)
                        vm.setLanguage(l)
                        vm.setDefaultMode(ReadingMode.AUDIO)
                    }
                )
            } else {
                vm.setLanguage(l)
            }
        }
    }

    Column(modifier.fillMaxSize()) {
        AppBar(
            title = step.title,
            subtitle = "$tag · шаг ${idx + 1} из ${phaseList.size} · " +
                if (marker) "отметка" else "≈ " + fmtMin(step.audioSeconds),
            onBack = onClose,
            actions = { CircleButton("Aa", onOpenSettings, fontSize = 13.sp) }
        )

        // Сегменты этапа: по одному на шаг, с долей пройденного
        Row(
            Modifier.fillMaxWidth().background(p.card).padding(start = 14.dp, end = 14.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val seg = RoundedCornerShape(2.dp)
            phaseList.forEach { s ->
                val isCur = s.step.id == step.id
                val f = if (isCur) shownFrac else s.frac
                val c = if (s.progress.mode == ReadingMode.AUDIO) p.ok else p.gold
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(seg)
                        .background(p.line)
                        .then(if (isCur) Modifier.border(2.dp, p.acc, seg) else Modifier)
                        .clickable { onOpenStep(s.step.id) }
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(f.coerceIn(0f, 1f)).background(c))
                }
            }
        }

        // Легенда: прогресс шага и этапа
        Column(
            Modifier.fillMaxWidth().background(p.card)
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Этот шаг: $stepPct%", color = p.mut, fontFamily = Sans, fontSize = 11.sp)
                Text("$tag: $phasePct%", color = p.mut, fontFamily = Sans, fontSize = 11.sp)
            }
            PBar(shownFrac, height = 4.dp, color = p.acc, modifier = Modifier.padding(top = 3.dp))
            PBar(phasePct / 100f, height = 4.dp, ok = after, modifier = Modifier.padding(top = 3.dp))
        }
        HLine()

        if (!marker) {
            // Пометка «шаг начат»
            if (locked) {
                Column(
                    Modifier.fillMaxWidth().background(p.hl).padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "🔒 Шаг начат: " +
                            (if (pr.mode == ReadingMode.AUDIO) "🎧 слушание, " +
                                (if (effLang == Language.MODERN) "совр. рус." else "церк.-слав.")
                            else "📖 чтение") +
                            " · $stepPct%. Закончите так же или начните шаг сначала.",
                        color = p.ink, fontFamily = Sans, fontSize = 12.sp, lineHeight = 17.sp
                    )
                    LinkText("↺ Начать шаг сначала") { confirm = restartConfirm }
                }
                HLine()
            }
            // Способ и язык
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!done) {
                    SegToggle(
                        listOf(
                            (if (locked && mode != ReadingMode.TEXT) "🔒 " else "📖 ") + "Читать",
                            (if (locked && mode != ReadingMode.AUDIO) "🔒 " else "🎧 ") + "Слушать"
                        ),
                        if (mode == ReadingMode.TEXT) 0 else 1,
                        onMode,
                        Modifier.weight(1f),
                        dimmed = if (locked) setOf(if (mode == ReadingMode.TEXT) 1 else 0) else emptySet()
                    )
                }
                SegToggle(
                    listOf("Совр.", "Церк."),
                    if (effLang == Language.MODERN) 0 else 1,
                    onLang,
                    Modifier.weight(if (done) 1f else 1.1f),
                    dimmed = if (!done && pr.mode == ReadingMode.AUDIO)
                        setOf(if (effLang == Language.MODERN) 1 else 0) else emptySet()
                )
            }
        }

        // Тело шага
        if (audio) {
            key(step.id, effLang) {
                AudioMode(
                    textKey = step.textKey,
                    title = step.title,
                    language = effLang,
                    stepLabel = "Шаг ${idx + 1} из ${phaseList.size}",
                    startFraction = if (pr.mode == ReadingMode.AUDIO) pr.audioFraction else 0f,
                    onProgress = { f -> vm.recordAudio(step.id, f, effLang) },
                    onFinished = { vm.finish(step.id, ReadingMode.AUDIO, effLang) },
                    followContent = { TextCard(step.subtitle, text, fontSizeSp) }
                )
            }
        } else {
            if (marker) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppCard {
                        Text("🙏", fontSize = 40.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text(
                            step.title, color = p.ink, fontFamily = Serif, fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                        if (step.subtitle.isNotBlank()) Note(step.subtitle, Modifier.fillMaxWidth(), TextAlign.Center)
                    }
                    AppCard {
                        Note(
                            "У этого шага нет текста для чтения и аудиозаписи. Прочитайте молитву мысленно, " +
                                "а затем нажмите «Готово»: шаг засчитывается только по этой кнопке."
                        )
                    }
                }
            } else {
                key(step.id) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(14.dp)
                    ) {
                        TextCard(step.subtitle, text, fontSizeSp)
                    }
                }
            }

            // Нижняя панель: «Прочитано» / «Готово» или переход дальше
            Column(Modifier.fillMaxWidth().background(p.card)) {
                HLine()
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!done) {
                        if (marker) {
                            AppButton("Готово ✓", { vm.finish(step.id, null, null) })
                        } else {
                            if (!canRead) {
                                Note(
                                    "Дочитайте до конца: тогда кнопка станет активной",
                                    Modifier.fillMaxWidth(), TextAlign.Center
                                )
                            }
                            AppButton(
                                "Прочитано ✓",
                                { vm.finish(step.id, ReadingMode.TEXT, null) },
                                enabled = canRead
                            )
                        }
                    } else {
                        val via = when (pr.mode) {
                            ReadingMode.AUDIO -> " · 🎧 прослушано"
                            ReadingMode.TEXT -> " · 📖 прочитано"
                            null -> ""
                        }
                        Text(
                            "Шаг пройден ✓$via", color = p.ok, fontFamily = Sans, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                        )
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
                        if (marker) {
                            AppButton("↺ Снять отметку", { vm.resetStep(step.id) }, outlined = true)
                        } else {
                            Note("Пройти шаг заново:", Modifier.fillMaxWidth(), TextAlign.Center)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppButton(
                                    "↺ 📖 Читать",
                                    { vm.resetStep(step.id); vm.setDefaultMode(ReadingMode.TEXT) },
                                    Modifier.weight(1f), outlined = true
                                )
                                AppButton(
                                    "↺ 🎧 Слушать",
                                    { vm.resetStep(step.id); vm.setDefaultMode(ReadingMode.AUDIO) },
                                    Modifier.weight(1f), outlined = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    confirm?.let { c -> ConfirmDialog(c) { confirm = null } }
}