package ru.example.prichastie.model

data class StepProgress(
    val stepId: String,
    /** null, пока шаг не начат. */
    val mode: ReadingMode? = null,
    val language: Language? = null,
    /** Дочитано до: 0.0 … 1.0 (для текста). */
    val readFraction: Float = 0f,
    /** Прослушано: 0.0 … 1.0 (для аудио). */
    val audioFraction: Float = 0f,
    val done: Boolean = false
)