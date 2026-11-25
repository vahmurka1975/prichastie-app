package ru.example.prichastie.updates

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {
    @Test fun comparesNumbersInsteadOfText() {
        assertTrue(ReleaseVersion.isNewer("v1.10.0", "1.9.0") == true)
        assertTrue(ReleaseVersion.isNewer("v1.9.0", "1.10.0") == false)
    }
    @Test fun ignoresPrefixAndTrailingZeroes() {
        assertTrue(ReleaseVersion.isNewer("v1.0.0", "1.0") == false)
        assertTrue(ReleaseVersion.isNewer("V1.0", "1.0.0.0") == false)
        assertTrue(ReleaseVersion.isNewer("v1.0.1", "1.0") == true)
    }
    @Test fun refusesUnsupportedVersions() {
        assertNull(ReleaseVersion.isNewer("v1.1-beta", "1.0"))
        assertNull(ReleaseVersion.isNewer("release-2", "1.0"))
        assertNull(ReleaseVersion.isNewer("2.0", "1.0-debug"))
        assertNull(ReleaseVersion.isNewer("9999999999999999999999999", "1.0"))
        assertNull(ReleaseVersion.isNewer("", "1.0"))
    }
}
