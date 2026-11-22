package ru.example.prichastie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {

    @Test
    fun idsAreUnique() {
        val ids = Catalog.steps.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyStepHasKnownGroup() {
        val groupIds = Catalog.groups.map { it.id }.toSet()
        assertTrue(Catalog.steps.all { it.groupId in groupIds })
    }

    @Test
    fun counts() {
        assertEquals(41, Catalog.steps.size)
        assertEquals(38, Catalog.activeSteps(includeCanons = false).size)
    }

    @Test
    fun creedIsOneStep() {
        assertEquals(1, Catalog.steps.count { it.textKey == "b28" })
    }

    @Test
    fun stagesGroupSteps() {
        val bySize = Catalog.stages.associate { it.id to Catalog.stepsOf(it).size }
        assertEquals(mapOf("canons" to 3, "sequence" to 25, "thanks" to 12), bySize)
        // Все шаги, кроме Причащения, принадлежат этапу
        assertTrue(Catalog.steps.filter { it.id != "communion" }.all { Catalog.stageOf(it) != null })
        assertNull(Catalog.byId("communion")!!.stageId)
    }

    @Test
    fun audioKeys() {
        fun key(id: String, v: LiturgyVariant? = null) = Catalog.audioKeyFor(Catalog.byId(id)!!, v)

        assertEquals("b01", key("canon_penitential"))
        assertEquals("b03", key("canon_angel"))
        assertEquals("b04", key("psalm_22"))
        assertEquals("b04", key("creed_church"))
        assertEquals("a01", key("thanks_2"))
        assertEquals("a01", key("thanks_troparion"))
        assertEquals("a12.v", key("thanks_troparion", LiturgyVariant.BASIL))
        assertEquals("a12.g", key("thanks_troparion", LiturgyVariant.GREGORY))
        assertNull(key("communion"))
    }

    @Test
    fun sharedAudioFlag() {
        assertFalse(Catalog.isSharedAudio(Catalog.byId("canon_angel")!!))
        assertTrue(Catalog.isSharedAudio(Catalog.byId("psalm_22")!!))
        assertTrue(Catalog.isSharedAudio(Catalog.byId("thanks_troparion")!!))
        assertFalse(Catalog.isSharedAudio(Catalog.byId("thanks_troparion")!!, LiturgyVariant.BASIL))
		assertTrue(Catalog.isSharedAudio(Catalog.byId("prep_tsaryu")!!))     // b04
        assertTrue(Catalog.isSharedAudio(Catalog.byId("thanks_glory")!!))    // a01
    }
}