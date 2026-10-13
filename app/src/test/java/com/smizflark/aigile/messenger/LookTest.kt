package com.smizflark.aigile.messenger

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.smizflark.aigile.messenger.ui.Look
import com.smizflark.aigile.messenger.ui.isDark
import com.smizflark.aigile.messenger.ui.schemeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookTest {
    @Test fun followsThePhoneByDefault() {
        assertEquals(Look.System, Look.of("nonsense"))
        assertTrue(isDark(Look.System, systemDark = true))
        assertFalse(isDark(Look.System, systemDark = false))
    }

    @Test fun chosenLookBeatsThePhone() {
        assertFalse(isDark(Look.Light, systemDark = true))
        assertTrue(isDark(Look.Dark, systemDark = false))
        assertTrue(isDark(Look.Black, systemDark = false))
    }

    @Test fun blackIsPureBlackEvenWithPhoneColours() {
        val phone = darkColorScheme(primary = Color(0xFF8AB4F8), surface = Color(0xFF1B1B1F))
        val s = schemeFor(Look.Black, systemDark = false, phone = phone)
        assertEquals(Color.Black, s.background)
        assertEquals(Color.Black, s.surface)
        assertEquals(phone.primary, s.primary)   // keeps the phone's accent
        assertNotEquals(Color.Black, s.surfaceContainerHighest)   // bars and bubbles still stand out
    }

    @Test fun darkIsNotBlack() {
        assertNotEquals(Color.Black, schemeFor(Look.Dark, systemDark = false, phone = null).background)
    }
}
