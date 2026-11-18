package ru.example.prichastie

import ru.example.prichastie.model.Phase
import kotlin.math.roundToInt

/** Шаги этапа, которые входят в выбранный режим и считаются в прогрессе. */
fun UiState.phaseSteps(phase: Phase): List<StepUi> =
    steps.filter { it.included && it.step.phase == phase && it.step.audioSeconds > 0 }

val UiState.communionDone: Boolean
    get() = steps.any { it.step.id == "communion" && it.done }

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
    val done = filter { it.done }.sumOf { it.step.audioSeconds.toLong() }
    val v = (done * 100f / total).roundToInt()
    return if (done < total && v > 99) 99 else v
}

fun List<StepUi>.remainingSec(): Int =
    filter { !it.done }.sumOf { it.step.audioSeconds }

fun fmtMin(sec: Int): String {
    val m = (sec / 60f).roundToInt()
    return when {
        m < 1 -> "<1 мин"
        m >= 60 -> "${m / 60} ч ${m % 60} мин"
        else -> "$m мин"
    }
}