package ru.example.prichastie.model

import kotlin.math.roundToInt

/** Правила подсчёта прогресса. Без Android, поэтому легко проверяются тестами. */
object ProgressMath {

    /** Начиная с этой доли шаг считается дочитанным или дослушанным до конца. */
    const val END_THRESHOLD = 0.98f

    /** Доля пройденного шага: 0.0 … 1.0. Для отмеченного шага всегда 1. */
    fun fraction(p: StepProgress): Float {
        if (p.done) return 1f
        val raw = when (p.mode) {
            ReadingMode.TEXT -> p.readFraction
            ReadingMode.AUDIO -> p.audioFraction
            null -> 0f
        }
        return raw.coerceIn(0f, 1f)
    }

    /** Проценты для кольца. Пока шаг не отмечен, больше 99 не показываем. */
    fun percent(p: StepProgress): Int {
        val v = (fraction(p) * 100).roundToInt()
        return if (p.done) 100 else v.coerceAtMost(99)
    }

    /** Режим «Читать / Слушать» можно менять, пока у шага нет прогресса. */
    fun isModeLocked(p: StepProgress): Boolean = p.done || fraction(p) > 0f

    /** Кнопка «Прочитано ✓» активна, когда текст или аудио дошли до конца. */
    fun canMarkDone(p: StepProgress): Boolean = p.done || fraction(p) >= END_THRESHOLD

    /** Доля прокрутки текста: value и max берутся из ScrollState. */
    fun scrollFraction(value: Int, max: Int): Float =
        if (max <= 0) 1f else (value.toFloat() / max).coerceIn(0f, 1f)

    /** Доля воспроизведения аудио. */
    fun audioFraction(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    /**
     * Общий прогресс набора шагов с весом по длительности.
     * Пары: (длительность шага в секундах, прогресс шага).
     */
    fun weighted(items: List<Pair<Int, StepProgress>>): Float {
        val total = items.sumOf { it.first.toLong() }
        if (total == 0L) return 0f
        val sum = items.sumOf { (sec, p) -> (sec * fraction(p)).toDouble() }
        return (sum / total).toFloat().coerceIn(0f, 1f)
    }
}