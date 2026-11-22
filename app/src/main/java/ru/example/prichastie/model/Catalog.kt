package ru.example.prichastie.model

/**
 * Каталог шагов. Поле textKey — это имя файла в assets/text/ без языка и расширения.
 * Пример: textKey = "b01" -> assets/text/b01.mod.txt и assets/text/b01.csl.txt.
 * Имена взяты из таблицы «Правило_ко_причастию_файлы».
 */
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
        textKey: String, kind: StepKind = StepKind.NORMAL
    ) = Step(id, group, phase, title, sub, sec, textKey, kind)

    private fun before(id: String, g: String, key: String, t: String, sub: String, sec: Int) =
        s(id, g, Phase.BEFORE, t, sub, sec, key)

    private fun after(id: String, key: String, t: String, sub: String, sec: Int,
                      kind: StepKind = StepKind.NORMAL) =
        s(id, "thanks", Phase.AFTER, t, sub, sec, key, kind)

    val steps: List<Step> = listOf(
        // ── Три канона (b01–b03) ──
        before("canon_penitential", "canons", "b01", "Покаянный канон ко Господу нашему Иисусу Христу", "", 840),
        before("canon_theotokos", "canons", "b02", "Канон молебный ко Пресвятой Богородице", "", 780),
        before("canon_angel", "canons", "b03", "Канон Ангелу Хранителю", "", 720),

        // ── Последование: начальные молитвы (b04–b09) ──
        before("prep_tsaryu", "prep", "b04", "«Царю Небесный»", "", 40),
        before("prep_trisagion", "prep", "b05", "Трисвятое", "«Святый Боже…»", 30),
        before("prep_trinity", "prep", "b06", "«Пресвятая Троице»", "«Пресвятая Троице, помилуй нас…»", 25),
        before("prep_kyrie", "prep", "b07", "«Господи, помилуй»", "12 раз", 30),
        before("prep_come", "prep", "b08", "«Приидите, поклонимся»", "трижды", 25),
        before("prep_ourfather", "prep", "b09", "«Отче наш»", "", 40),

        // ── Последование (b10–b26) ──
        before("psalm_22", "prep", "b10", "Псалом 22", "«Господь пасет мя»", 70),
        before("psalm_23", "prep", "b11", "Псалом 23", "«Господня земля»", 80),
        before("psalm_115", "prep", "b12", "Псалом 115", "«Веровах, тем же возглаголах»", 80),
        before("prep_troparia", "prep", "b13", "Тропари", "«Беззакония моя презри, Господи…», Слава, Богородичен", 110),
        before("psalm_50", "prep", "b14", "Псалом 50", "покаянный", 150),
        before("canon_jesus", "prep", "b15", "Канон Иисусу Сладчайшему",
            "«Водою многих»; в конце кондак «Хлеб, Христе…», ирмос «Якоже огнь…», затем «Достойно есть»", 900),
        before("prep_troparia_penitential", "prep", "b16", "Тропари покаянные",
            "«Помилуй нас, Господи, помилуй нас…» со Славой и Богородичным", 100),
        before("prayer_b1", "prep", "b17", "Молитва 1-я", "святителя Василия Великого", 170),
        before("prayer_b2", "prep", "b18", "Молитва 2-я", "святителя Иоанна Златоуста", 170),
        before("prayer_b3", "prep", "b19", "Молитва 3-я", "преподобного Симеона Метафраста", 170),
        before("prayer_b4", "prep", "b20", "Молитва 4-я", "преподобного Иоанна Дамаскина", 170),
        before("prayer_b5", "prep", "b21", "Молитва 5-я", "святителя Василия Великого", 170),
        before("prayer_b6", "prep", "b22", "Молитва 6-я", "преподобного Симеона Нового Богослова («От скверных уст…»)", 170),
        before("prayer_b7", "prep", "b23", "Молитва 7-я", "святителя Иоанна Златоуста", 170),
        before("prayer_b8", "prep", "b24", "Молитва 8-я", "святителя Иоанна Златоуста («Несмь доволен, Владыко Господи…»)", 170),
        before("prayer_b9", "prep", "b25", "Молитва 9-я", "святителя Иоанна Златоуста", 170),
        before("prayer_b10", "prep", "b26", "Молитва 10-я", "преподобного Иоанна Дамаскина", 170),
        // Символ веры: в таблице один файл b28, читается дважды — оба шага берут его.
        before("creed_evening", "prep", "b28", "Символ веры", "вечером, в конце последования", 70),

        // ── В храме (b27, b28) ──
        before("church_prayer", "church", "b27", "Молитва «Приидите, ядите, сие есть Тело Мое…»",
            "святителя Иоанна Златоуста; читается мысленно, подходя к чаше", 60),
        before("creed_church", "church", "b28", "Символ веры", "второй раз: в храме, перед самым причастием", 70),

        // ── Само Причащение (в таблице: comm, файлов нет) ──
        s("communion", "communion", Phase.COMMUNION, "Причащение Святых Таин",
            "само Таинство, под песнь «Тело Христово приимите…»", 0, "comm", StepKind.COMMUNION),

        // ── Благодарственные молитвы (a01–a18) ──
        after("thanks_glory", "a01", "«Слава Тебе, Боже»", "трижды, сразу после причастия", 25),
        after("thanks_1", "a02", "Благодарственная молитва 1-я", "«Благодарю Тя, Господи, Боже мой, яко не отринул мя еси грешнаго…»", 170),
        after("thanks_2", "a03", "Молитва 2-я", "святителя Василия Великого («Владыко Христе Боже, Царю веков…»)", 170),
        after("thanks_3", "a04", "Молитва 3-я", "преподобного Симеона Метафраста («Давый пищу мне плоть Твою волею…»)", 170),
        after("thanks_4", "a05", "Молитва 4-я", "«Тело Твое Святое, Господи Иисусе Христе Боже наш…»", 150),
        after("thanks_5", "a06", "Молитва 5-я", "ко Пресвятой Богородице («Пресвятая Владычице Богородице, свете помраченныя моея души…»)", 170),
        after("thanks_simeon", "a07", "«Ныне отпущаеши»", "молитва Симеона Богоприимца", 90),
        after("thanks_trisagion", "a08", "Трисвятое", "«Святый Боже…»", 30),
        after("thanks_trinity", "a09", "«Пресвятая Троице»", "«Пресвятая Троице, помилуй нас…»", 25),
        after("thanks_kyrie", "a10", "«Господи, помилуй»", "12 раз", 30),
        after("thanks_ourfather", "a11", "«Отче наш»", "", 40),
        // Три файла: a12.z, a12.v, a12.g. По умолчанию a12.z (см. textKeyFor ниже).
        after("thanks_troparion", "a12.z", "Тропарь и кондак",
            "по совершившейся литургии: святителю Иоанну Златоусту, Василию Великому или Григорию Двоеслову",
            70, StepKind.LITURGY_DEPENDENT),
        after("thanks_6", "a13", "Молитва 6-я", "святителя Иоанна Златоуста", 170),
        after("thanks_7", "a14", "Молитва 7-я", "ко Пресвятой Богородице", 170),
        after("thanks_8", "a15", "Молитва 8-я", "Господу нашему Иисусу Христу", 170),
        after("thanks_9", "a16", "Молитва 9-я", "к Ангелу Хранителю", 170),
        after("thanks_10", "a17", "Молитва 10-я", "ко Пресвятой Богородице", 170),
        after("thanks_end", "a18", "Окончание чина",
            "«Предстательство христиан непостыдное…», «Господи, помилуй» ×12, «Честнейшую Херувим…»", 90)
    )

    private val optionalGroupIds = groups.filter { it.optional }.map { it.id }.toSet()

    /** Шаги с учётом настройки «читать три канона». */
    fun activeSteps(includeCanons: Boolean): List<Step> =
        if (includeCanons) steps else steps.filter { it.groupId !in optionalGroupIds }

    fun byId(id: String): Step? = steps.firstOrNull { it.id == id }

    /** Буква варианта в имени файла тропаря: a12.z / a12.v / a12.g. */
    private fun liturgyLetter(v: LiturgyVariant) = when (v) {
        LiturgyVariant.CHRYSOSTOM -> "z"
        LiturgyVariant.BASIL -> "v"
        LiturgyVariant.GREGORY -> "g"
    }

    /**
     * Какой файл читать для шага. Для обычных шагов это step.textKey.
     * Для тропаря по литургии подставляется вариант (если не выбран — Златоуст).
     */
    fun textKeyFor(step: Step, variant: LiturgyVariant?): String =
        if (step.kind == StepKind.LITURGY_DEPENDENT)
            "a12." + liturgyLetter(variant ?: LiturgyVariant.CHRYSOSTOM)
        else step.textKey
}