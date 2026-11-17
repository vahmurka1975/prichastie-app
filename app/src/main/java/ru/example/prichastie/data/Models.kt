package ru.example.prichastie.data

enum class Phase { BEFORE, AFTER }

/** NORMAL — обычный шаг, MARK — шаг-отметка «Готово», TROPAR — тропарь и кондак (зависит от литургии) */
enum class Kind { NORMAL, MARK, TROPAR }

data class Group(
    val id: Int,
    val phase: Phase,
    val title: String,
    val subtitle: String,
    val canon: Boolean = false
)

data class Step(
    val id: String,
    val phase: Phase,
    val group: Int,
    val title: String,
    val subtitle: String,
    val durationSec: Int,
    val kind: Kind,
    val fileKey: String
)