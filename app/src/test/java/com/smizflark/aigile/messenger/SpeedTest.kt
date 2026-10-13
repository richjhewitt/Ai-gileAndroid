package com.smizflark.aigile.messenger

import androidx.compose.ui.graphics.Color
import com.smizflark.aigile.messenger.ui.Markdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Speed (AII-37): long chats scroll without re-parsing, and squeeze stays linear. */
class SpeedTest {

    /* ---------- rendered messages are cached ---------- */

    @Test fun sameMessageRendersOnce() =
        assertSame(Markdown.render("- **hi** `x`", Color.Gray, Color.Blue), Markdown.render("- **hi** `x`", Color.Gray, Color.Blue))

    @Test fun otherColoursRenderAgain() =
        assertNotSame(Markdown.render("dark mode", Color.Gray, Color.Blue), Markdown.render("dark mode", Color.Black, Color.Blue))

    @Test fun cachedRenderKeepsText() =
        assertEquals("•  bold and code", Markdown.render("- **bold** and `code`", Color.Gray, Color.Blue).text)

    /* ---------- squeeze: same output, no quadratic look-ahead ---------- */

    @Test fun squeezeStillDropsBlankLinesBetweenBullets() =
        assertEquals("- a\n- b\n\ntext", Markdown.squeeze("- a\n\n- b\n\n\n\ntext"))

    @Test fun squeezeLongMessageIsFast() {
        val long = (1..20_000).joinToString("\n\n") { "- line $it" }
        val t = System.nanoTime()
        val out = Markdown.squeeze(long)
        assertEquals(20_000, out.lines().size)
        assertTrue("took ${(System.nanoTime() - t) / 1_000_000} ms", System.nanoTime() - t < 2_000_000_000)
    }
}
