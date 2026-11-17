package ru.example.prichastie.model

object Catalog {

    val groups: List<StepGroup> = listOf(
        StepGroup("canons", Phase.BEFORE, "Три канона",
            "вечер накануне или в дни подготовки · по желанию", optional = true),
        StepGroup("prep", Phase.BEFORE, "Последование ко Святому Причащению",
            "вечер накануне или утром в день Причастия"),
        StepGroup("church", Phase.BEFORE, "В храме перед Причастием",
            "в день Причастия, перед самой чашей"),
        StepGroup("communion", Phase.COMMUNION, "Причащение Святых Таин",
            "само Таинство"),
        StepGroup("thanks", Phase.AFTER, "Благодарственные молитвы по Святом Причащении",
            "сразу по выходе из храма или дома")
    )

    private fun s(
        id: String, group: String, phase: Phase, title: String, sub: String, sec: Int,
        textKey: String = id, kind: StepKind = StepKind.NORMAL
    ) = Step(id, group, phase, title, sub, sec, textKey, kind)

    private fun before(id: String, g: String, t: String, sub: String, sec: Int, key: String = id) =
        s(id, g, Phase.BEFORE, t, sub, sec, key)

    private fun after(id: String, t: String, sub: String, sec: Int,
                      kind: StepKind = StepKind.NORMAL) =
        s(id, "thanks", Phase.AFTER, t, sub, sec, id, kind)

    val steps: List<Step> = listOf(
        // Три канона
        before("canon_penitential", "canons", "Покаянный канон ко Господу нашему Иисусу Христу", "", 840),
        before("canon_theotokos", "canons", "Канон молебный ко Пресвятой Богородице", "", 780),
        before("canon_angel", "canons", "Канон Ангелу Хранителю", "", 720),

        // Последование ко Святому Причащению
        before("prep_opening", "prep", "Начальные молитвы",
            "«Царю Небесный», Трисвятое, «Пресвятая Троице», «Господи, помилуй» ×12, «Приидите, поклонимся», «Отче наш»", 150),
        before("psalm_22", "prep", "Псалом 22", "«Господь пасет мя»", 70),
        before("psalm_23", "prep", "Псалом 23", "«Господня земля»", 80),
        before("psalm_115", "prep", "Псалом 115", "«Веровах, тем же возглаголах»", 80),
        before("prep_troparia", "prep", "Тропари", "«Беззакония моя презри, Господи…», Слава, Богородичен", 110),
        before("psalm_50", "prep", "Псалом 50", "покаянный", 150),
        before("canon_jesus", "prep", "Канон Иисусу Сладчайшему",
            "«Водою многих»; в конце кондак «Хлеб, Христе…», ирмос «Якоже огнь…», затем «Достойно есть»", 900),
        before("prep_troparia_penitential", "prep", "Тропари покаянные",
            "«Помилуй нас, Господи, помилуй нас…» со Славой и Богородичным", 100),
        before("prayer_b1", "prep", "Молитва 1-я", "святителя Василия Великого", 170),
        before("prayer_b2", "prep", "Молитва 2-я", "святителя Иоанна Златоуста", 170),
        before("prayer_b3", "prep", "Молитва 3-я", "преподобного Симеона Метафраста", 170),
        before("prayer_b4", "prep", "Молитва 4-я", "преподобного Иоанна Дамаскина", 170),
        before("prayer_b5", "prep", "Молитва 5-я", "святителя Василия Великого", 170),
        before("prayer_b6", "prep", "Молитва 6-я", "преподобного Симеона Нового Богослова («От скверных уст…»)", 170),
        before("prayer_b7", "prep", "Молитва 7-я", "святителя Иоанна Златоуста", 170),
        before("prayer_b8", "prep", "Молитва 8-я", "его же («Несмь доволен, Владыко Господи…»)", 170),
        before("prayer_b9", "prep", "Молитва 9-я", "его же", 170),
        before("prayer_b10", "prep", "Молитва 10-я", "преподобного Иоанна Дамаскина", 170),
        before("creed_evening", "prep", "Символ веры", "вечером, в конце последования", 70, key = "creed"),

        // В храме
        before("church_prayer", "church", "Молитва «Приидите, ядите, сие есть Тело Мое…»",
            "святителя Иоанна Златоуста; читается мысленно, подходя к чаше", 30),
        before("creed_church", "church", "Символ веры", "второй раз: в храме, перед самым причастием", 70, key = "creed"),

        // Само Причащение
        s("communion", "communion", Phase.COMMUNION, "Причащение Святых Таин",
            "само Таинство, под песнь «Тело Христово приимите…»", 0, kind = StepKind.COMMUNION),

        // Благодарственные молитвы
        after("thanks_glory", "«Слава Тебе, Боже»", "трижды, сразу после причастия", 25),
        after("thanks_1", "Благодарственная молитва 1-я", "«Благодарю Тя, Господи, Боже мой, яко не отринул мя еси грешнаго…»", 170),
        after("thanks_2", "Молитва 2-я", "святителя Василия Великого («Владыко Христе Боже, Царю веков…»)", 170),
        after("thanks_3", "Молитва 3-я", "преподобного Симеона Метафраста («Давый пищу мне плоть Твою волею…»)", 170),
        after("thanks_4", "Молитва 4-я", "«Тело Твое Святое, Господи Иисусе Христе Боже наш…»", 150),
        after("thanks_5", "Молитва 5-я", "ко Пресвятой Богородице («Пресвятая Владычице Богородице, свете помраченныя моея души…»)", 170),
        after("thanks_simeon", "«Ныне отпущаеши»", "молитва Симеона Богоприимца", 90),
        after("thanks_trisagion", "Трисвятое… «Отче наш»", "Трисвятое, «Пресвятая Троице», «Господи, помилуй» ×12, «Отче наш»", 100),
        after("thanks_troparion", "Тропарь и кондак",
            "по совершившейся литургии: святителю Иоанну Златоусту, Василию Великому или Григорию Двоеслову",
            70, StepKind.LITURGY_DEPENDENT),
        after("thanks_6", "Молитва 6-я", "святителя Иоанна Златоуста", 170),
        after("thanks_7", "Молитва 7-я", "ко Пресвятой Богородице", 170),
        after("thanks_8", "Молитва 8-я", "Господу нашему Иисусу Христу", 170),
        after("thanks_9", "Молитва 9-я", "к Ангелу Хранителю", 170),
        after("thanks_10", "Молитва 10-я", "ко Пресвятой Богородице", 170),
        after("thanks_end", "Окончание чина",
            "«Предстательство христиан непостыдное…», «Господи, помилуй» ×12, «Честнейшую Херувим…»", 90)
    )

    private val optionalGroupIds = groups.filter { it.optional }.map { it.id }.toSet()

    /** Шаги с учётом настройки «читать три канона». */
    fun activeSteps(includeCanons: Boolean): List<Step> =
        if (includeCanons) steps else steps.filter { it.groupId !in optionalGroupIds }

    fun byId(id: String): Step? = steps.firstOrNull { it.id == id }
}