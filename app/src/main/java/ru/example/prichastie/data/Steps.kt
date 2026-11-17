package ru.example.prichastie.data

import ru.example.prichastie.model.Phase
import ru.example.prichastie.model.ReadingMethod
import ru.example.prichastie.model.Step

object Steps {
    val all = listOf(
        Step("canon1", "Канон 1 (заглушка)", Phase.BEFORE, "Каноны", ReadingMethod.TEXT),
        Step("canon2", "Канон 2 (заглушка)", Phase.BEFORE, "Каноны", ReadingMethod.TEXT),
        Step("canon3", "Канон 3 (заглушка)", Phase.BEFORE, "Каноны", ReadingMethod.TEXT),
        Step("prayer1", "Молитва 1 (заглушка)", Phase.BEFORE, "Молитвы", ReadingMethod.TEXT),
        Step("prayer2", "Молитва 2 (заглушка)", Phase.BEFORE, "Молитвы", ReadingMethod.TEXT),
        Step("come", "Приидите, ядите… (заглушка)", Phase.BEFORE, "Перед чашей",
            ReadingMethod.MARK_ONLY, countsInProgress = false),
        Step("prichastie", "Причастие", Phase.AFTER, "Причастие",
            ReadingMethod.MARK_ONLY, countsInProgress = false),
        Step("thanks1", "Благодарственная молитва 1 (заглушка)", Phase.AFTER, "Благодарение", ReadingMethod.TEXT),
        Step("thanks2", "Благодарственная молитва 2 (заглушка)", Phase.AFTER, "Благодарение", ReadingMethod.TEXT)
    )
}