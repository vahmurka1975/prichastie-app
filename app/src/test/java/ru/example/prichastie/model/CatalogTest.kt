package ru.example.prichastie.model

import org.junit.Assert.assertEquals
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
        assertEquals(40, Catalog.steps.size)
        assertEquals(37, Catalog.activeSteps(includeCanons = false).size)
    }

    @Test
    fun creedSharesOneText() {
        val keys = Catalog.steps.filter { it.title == "Символ веры" }.map { it.textKey }
        assertEquals(listOf("creed", "creed"), keys)
    }
}