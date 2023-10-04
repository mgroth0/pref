package matt.pref.test


import matt.pref.safepref.SafePref
import matt.test.assertions.JupiterTestAssertions.assertRunsInOneMinute
import kotlin.test.Test
import kotlin.test.assertEquals

class PrefTests {
    @Test
    fun setAndGetProperty() = assertRunsInOneMinute {
        val prop = SafePref("matt.pref.test.pref")
        prop.putInt("testInt", 3)
        assertEquals(3, prop.getInt("testInt", 0))
    }
}