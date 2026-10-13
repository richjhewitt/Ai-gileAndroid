package com.smizflark.aigile.messenger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.smizflark.aigile.messenger.ui.Dark
import com.smizflark.aigile.messenger.ui.Light
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-32: text colours on older phones (no Material You) pass WCAG AA, 4.5:1 for normal text. */
class ContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun aa(name: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue("$name is %.2f:1, needs 4.5:1".format(r), r >= 4.5)
    }

    @Test fun lightScheme() = with(Light) {
        aa("button text on primary", onPrimary, primary)
        aa("primary text on surface", primary, surface)
        aa("my bubble text", onPrimaryContainer, primaryContainer)
        aa("body text", onSurface, surface)
        aa("grey text", onSurfaceVariant, surface)
    }

    @Test fun darkScheme() = with(Dark) {
        aa("button text on primary", onPrimary, primary)
        aa("primary text on surface", primary, surface)
        aa("my bubble text", onPrimaryContainer, primaryContainer)
        aa("body text", onSurface, surface)
        aa("grey text", onSurfaceVariant, surface)
    }
}
