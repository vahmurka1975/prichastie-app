package ru.example.prichastie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.example.prichastie.model.ProgressMath
import ru.example.prichastie.model.ReadingMode
import ru.example.prichastie.model.StepProgress

class ProgressMathTest {

    private fun p(
        mode: ReadingMode? = null, read: Float = 0f, audio: Float = 0f, done: Boolean = false
    ) = StepProgress("x", mode, null, read, audio, done)

    @Test fun notStartedStepHasZeroProgressAndUnlockedMode() {
        val s = p()
        assertEquals(0f, ProgressMath.fraction(s), 0.001f)
        assertFalse(ProgressMath.isModeLocked(s))
    }

    @Test fun textModeUsesReadFraction() {
        val s = p(ReadingMode.TEXT, read = 0.4f, audio = 0.9f)
        assertEquals(0.4f, ProgressMath.fraction(s), 0.001f)
    }

    @Test fun audioModeUsesAudioFraction() {
        val s = p(ReadingMode.AUDIO, read = 0.9f, audio = 0.25f)
        assertEquals(0.25f, ProgressMath.fraction(s), 0.001f)
    }

    @Test fun modeLocksAsSoonAsThereIsProgress() {
        assertTrue(ProgressMath.isModeLocked(p(ReadingMode.TEXT, read = 0.01f)))
    }

    @Test fun doneStepIsFullAndLocked() {
        val s = p(ReadingMode.TEXT, read = 0.3f, done = true)
        assertEquals(1f, ProgressMath.fraction(s), 0.001f)
        assertEquals(100, ProgressMath.percent(s))
        assertTrue(ProgressMath.isModeLocked(s))
    }

    @Test fun percentNeverShows100UntilDone() {
        assertEquals(99, ProgressMath.percent(p(ReadingMode.TEXT, read = 0.999f)))
    }

    @Test fun markDoneNeedsEndOfContent() {
        assertFalse(ProgressMath.canMarkDone(p(ReadingMode.TEXT, read = 0.5f)))
        assertTrue(ProgressMath.canMarkDone(p(ReadingMode.TEXT, read = 0.99f)))
        assertTrue(ProgressMath.canMarkDone(p(ReadingMode.AUDIO, audio = 1f)))
    }

    @Test fun scrollFractionHandlesShortText() {
        assertEquals(1f, ProgressMath.scrollFraction(0, 0), 0.001f)
        assertEquals(0.5f, ProgressMath.scrollFraction(50, 100), 0.001f)
    }

    @Test fun weightedProgressUsesDuration() {
        val items = listOf(
            100 to p(done = true),
            300 to p(ReadingMode.AUDIO, audio = 0.5f)
        )
        // (100 + 150) / 400
        assertEquals(0.625f, ProgressMath.weighted(items), 0.001f)
    }
}