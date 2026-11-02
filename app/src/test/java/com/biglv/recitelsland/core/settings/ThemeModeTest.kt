package com.biglv.recitelsland.core.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun invalidOrEmptyFallsBackToSystem() {
        assertEquals(ThemeMode.System, ThemeMode.fromRaw(null))
        assertEquals(ThemeMode.System, ThemeMode.fromRaw(""))
        assertEquals(ThemeMode.System, ThemeMode.fromRaw("unknown"))
    }

    @Test
    fun persistedNamesRoundTrip() {
        assertEquals(ThemeMode.Light, ThemeMode.fromRaw("Light"))
        assertEquals(ThemeMode.Dark, ThemeMode.fromRaw("Dark"))
        assertEquals(ThemeMode.System, ThemeMode.fromRaw("System"))
    }
}
