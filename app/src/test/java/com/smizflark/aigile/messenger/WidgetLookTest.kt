package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.ui.Look
import com.smizflark.aigile.messenger.widget.WidgetLook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WidgetLookTest {
    @Test fun lightIsLightWhateverThePhoneSays() {
        assertEquals(0, WidgetLook.colours(Look.Light, true).bg)
        assertEquals(0, WidgetLook.colours(Look.Light, false).bg)
    }
    @Test fun darkAndBlackAreDarkWhateverThePhoneSays() {
        assertEquals(1, WidgetLook.colours(Look.Dark, false).bg)
        assertEquals(2, WidgetLook.colours(Look.Black, false).bg)
        assertEquals(2, WidgetLook.colours(Look.Black, true).bg)
    }
    @Test fun followThePhoneUsesNightMode() {
        assertEquals(1, WidgetLook.colours(Look.System, true).bg)
        assertEquals(0, WidgetLook.colours(Look.System, false).bg)
    }
    @Test fun textDiffersBetweenLightAndDark() {
        assertNotEquals(WidgetLook.colours(Look.Light, false).title, WidgetLook.colours(Look.Dark, false).title)
    }
}
