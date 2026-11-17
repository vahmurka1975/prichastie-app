package ru.example.prichastie.model

// Этап: когда читается шаг
enum class Phase(val title: String) {
    BEFORE("До Причастия"),
    AFTER("После Причастия")
}

// Способ чтения: текст или простая отметка
enum class ReadingMethod {
    TEXT,       // шаг с текстом (позже появится и аудио)
    MARK_ONLY   // шаг без текста, отмечается кнопкой
}

data class Step(
    val id: String,
    val title: String,
    val phase: Phase,
    val group: String,                  // например "Каноны" или "Молитвы"
    val method: ReadingMethod,
    val countsInProgress: Boolean = true
)