package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.PicEdit
import com.smizflark.aigile.messenger.data.PicEdit.Corner
import com.smizflark.aigile.messenger.data.PicEdit.Crop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AII-385: crop maths for the picture editor. */
class PicEditTest {
    @Test fun fourQuarterTurnsComeBack() {
        val c = Crop(0.1f, 0.2f, 0.6f, 0.9f)
        var r = c; repeat(4) { r = PicEdit.rotateCw(r) }
        assertEquals(c.l, r.l, 1e-6f); assertEquals(c.t, r.t, 1e-6f); assertEquals(c.r, r.r, 1e-6f); assertEquals(c.b, r.b, 1e-6f)
    }
    @Test fun turnMovesTopLeftToTopRight() {
        assertEquals(Crop(0.75f, 0f, 1f, 0.5f), PicEdit.rotateCw(Crop(0f, 0f, 0.5f, 0.25f)))
    }
    @Test fun sizeSwapsOnOddTurns() {
        assertEquals(40 to 100, PicEdit.rotatedSize(100, 40, 1))
        assertEquals(100 to 40, PicEdit.rotatedSize(100, 40, 2))
        assertEquals(40 to 100, PicEdit.rotatedSize(100, 40, -1))
    }
    @Test fun clampKeepsInsideAndMinimum() {
        val k = PicEdit.clamp(Crop(-1f, 0.5f, 0.52f, 3f))
        assertEquals(0f, k.l, 0f); assertEquals(1f, k.b, 0f)
        assertTrue(k.w >= PicEdit.MIN - 1e-6f)
    }
    @Test fun dragKeepsOppositeCornerAndMinimum() {
        val c = Crop(0.2f, 0.2f, 0.8f, 0.8f)
        assertEquals(Crop(0f, 0.3f, 0.8f, 0.8f), PicEdit.drag(c, Corner.TL, -0.5f, 0.3f))
        val tiny = PicEdit.drag(c, Corner.BR, 0.1f, 0.1f)
        assertEquals(0.2f, tiny.l, 0f); assertEquals(0.3f, tiny.r, 1e-6f); assertEquals(0.3f, tiny.b, 1e-6f)
        assertEquals(1f, PicEdit.drag(c, Corner.BR, 5f, 5f).r, 0f)
    }
    @Test fun nearestCornerNeedsReach() {
        val c = Crop(0.1f, 0.1f, 0.9f, 0.9f)
        assertEquals(Corner.BR, PicEdit.nearest(c, 890f, 880f, 1000f, 1000f, 60f))
        assertNull(PicEdit.nearest(c, 500f, 500f, 1000f, 1000f, 60f))
    }
    @Test fun pixelsMapAndStayInside() {
        assertEquals(PicEdit.Px(100, 50, 400, 100), PicEdit.toPixels(Crop(0.1f, 0.25f, 0.5f, 0.75f), 1000, 200))
        assertEquals(PicEdit.Px(0, 0, 1000, 200), PicEdit.toPixels(PicEdit.FULL, 1000, 200))
    }
    @Test fun pngStaysPng() {
        assertTrue(PicEdit.isPng("a.PNG")); assertFalse(PicEdit.isPng("a.jpg")); assertFalse(PicEdit.isPng("png"))
        assertEquals("png", PicEdit.extFor("x.png")); assertEquals("jpg", PicEdit.extFor("x.webp"))
        assertEquals("n1-cat photo.jpg", PicEdit.copyName("ab12cd34-cat photo.jpeg", "n1"))
        assertEquals("n1-s.png", PicEdit.copyName("ab12cd34-s.png", "n1"))
    }
}
