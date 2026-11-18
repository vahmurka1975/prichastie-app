package ru.example.prichastie

import ru.example.prichastie.model.Phase
import ru.example.prichastie.model.ProgressMath
import ru.example.prichastie.model.ReadingMode
import ru.example.prichastie.model.Step
import kotlin.math.roundToInt

/** Шаги этапа, которые входят в выбранный режим и считаются в прогрессе. */
fun UiState.phaseSteps(phase: Phase): List<StepUi> =
    steps.filter { it.included && it.step.phase == phase && it.step.audioSeconds > 0 }

val UiState.communionDone: Boolean
    get() = steps.any { it.step.id == "communion" && it.done }

/** Шаг без текста и записи: засчитывается только кнопкой «Готово». */
val Step.isMarker: Boolean get() = id == "church_prayer"

/** Доля шага: 1 у пройденного, иначе не больше 0.99. */
val StepUi.frac: Float
    get() = if (done) 1f else ProgressMath.fraction(progress).coerceAtMost(0.99f)

fun StepUi.pct(): Int = (frac * 100).roundToInt()

/** Правая подпись строки шага: способ и проценты. */
fun StepUi.rowTrailing(): String {
    val icon = when (progress.mode) {
        ReadingMode.AUDIO -> "🎧\n"
        ReadingMode.TEXT -> "📖\n"
        null -> ""
    }
    return icon + when {
        done -> "100%"
        frac > 0f -> "${pct()}%"
        step.isMarker -> "отметка"
        else -> "≈ " + fmtMin(step.audioSeconds)
    }
}

/** Номер шага внутри своего этапа (как в макете: у каждого этапа счёт с 1). */
fun UiState.phaseNumbers(): Map<String, Int> {
    val m = HashMap<String, Int>()
    listOf(Phase.BEFORE, Phase.AFTER).forEach { ph ->
        phaseSteps(ph).forEachIndexed { i, s -> m[s.step.id] = i + 1 }
    }
    return m
}

/** Процент, взвешенный по времени звучания; 99, пока не всё пройдено. */
fun List<StepUi>.percent(): Int {
    val total = sumOf { it.step.audioSeconds.toLong() }
    if (total == 0L) return 0
    val weighted = sumOf { it.step.audioSeconds * it.frac.toDouble() }
    val v = (weighted * 100.0 / total).roundToInt()
    return if (!all { it.done } && v > 99) 99 else v
}

fun List<StepUi>.remainingSec(): Int =
    sumOf { it.step.audioSeconds * (1.0 - it.frac) }.roundToInt()

data class Mix(val read: Int, val heard: Int, val marked: Int)

/** Сколько пройденных шагов прочитано, прослушано и отмечено кнопкой. */
fun List<StepUi>.mix() = Mix(
    read = count { it.done && it.progress.mode == ReadingMode.TEXT },
    heard = count { it.done && it.progress.mode == ReadingMode.AUDIO },
    marked = count { it.done && it.progress.mode == null }
)

fun List<StepUi>.mixLine(): String {
    val m = mix()
    return "📖 прочитано ${m.read} · 🎧 прослушано ${m.heard}" +
        if (m.marked > 0) " · ✓ отмечено ${m.marked}" else ""
}

fun fmtMin(sec: Int): String {
    val m = (sec / 60f).roundToInt()
    return when {
        m < 1 -> "<1 мин"
        m >= 60 -> "${m / 60} ч ${m % 60} мин"
        else -> "$m мин"
    }
}