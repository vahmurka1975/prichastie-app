package ru.example.prichastie.model

/** Когда читается шаг: до Причастия, само Причащение, после. */
enum class Phase { BEFORE, COMMUNION, AFTER }

/** Язык текста и записи. */
enum class Language { MODERN, CHURCH_SLAVONIC }

/** Способ прохождения шага. Фиксируется при начале шага и не меняется. */
enum class ReadingMode { TEXT, AUDIO }

/** Особое поведение шага. */
enum class StepKind {
    NORMAL,
    /** Тропарь и кондак зависят от литургии (см. LiturgyVariant). */
    LITURGY_DEPENDENT,
    /** Само Таинство: чтения нет, пользователь только подтверждает. */
    COMMUNION
}

/** Какая литургия совершалась. Нужна для тропаря и кондака. */
enum class LiturgyVariant(val shortName: String) {
    CHRYSOSTOM("Иоанн Златоуст"),
    BASIL("Василий Великий"),
    GREGORY("Григорий Двоеслов")
}

/**
 * Этап: внутреннее понятие, которое объединяет шаги.
 * sharedAudioKey == null — у каждого шага своя запись (имя = textKey шага).
 * Иначе у всех шагов этапа одна общая запись: audio/<sharedAudioKey>.<mod|csl>.mp3.
 */
data class Stage(
    val id: String,
    val title: String,
    val sharedAudioKey: String? = null
)

data class StepGroup(
    val id: String,
    val phase: Phase,
    val title: String,
    val subtitle: String,
    /** Необязательная группа (три канона): её можно отключить в настройках. */
    val optional: Boolean = false
)

data class Step(
    val id: String,
    val groupId: String,
    val phase: Phase,
    val title: String,
    val subtitle: String,
    /** Длительность чтения в секундах (нужна для взвешенного прогресса). */
    val audioSeconds: Int,
    /** Ключ текста: имя файла в assets/text/ без языка и расширения. */
    val textKey: String = id,
    val kind: StepKind = StepKind.NORMAL,
    /** Этап, к которому относится шаг (null у Причащения). */
    val stageId: String? = null
)