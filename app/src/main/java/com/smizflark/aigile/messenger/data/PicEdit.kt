package com.smizflark.aigile.messenger.data

/** Crop and rotate a picked picture before sending (AII-385): the pure maths; ui/PicEditor.kt draws it. */
object PicEdit {
    /** A crop rectangle as fractions (0..1) of the picture as it is shown now (after rotating). */
    data class Crop(val l: Float, val t: Float, val r: Float, val b: Float) {
        val w get() = r - l
        val h get() = b - t
    }
    enum class Corner { TL, TR, BL, BR }
    /** A crop in bitmap pixels. */
    data class Px(val x: Int, val y: Int, val w: Int, val h: Int)

    val FULL = Crop(0f, 0f, 1f, 1f)
    const val MIN = 0.1f   // the smallest side of the crop, as a fraction of the picture

    /** The same crop after the picture turns a quarter clockwise. */
    fun rotateCw(c: Crop) = Crop(1f - c.b, c.l, 1f - c.t, c.r)

    /** Size after turning `quarters` quarter turns clockwise. */
    fun rotatedSize(w: Int, h: Int, quarters: Int): Pair<Int, Int> = if (Math.floorMod(quarters, 2) == 1) h to w else w to h

    /** Inside the picture and at least [MIN] on each side. */
    fun clamp(c: Crop): Crop {
        val l = c.l.coerceIn(0f, 1f - MIN); val t = c.t.coerceIn(0f, 1f - MIN)
        return Crop(l, t, c.r.coerceIn(l + MIN, 1f), c.b.coerceIn(t + MIN, 1f))
    }

    /** Drag one corner to (x, y), fractions of the picture; the opposite corner stays put. */
    fun drag(c: Crop, corner: Corner, x: Float, y: Float): Crop {
        val fx = x.coerceIn(0f, 1f); val fy = y.coerceIn(0f, 1f)
        return when (corner) {
            Corner.TL -> Crop(fx.coerceAtMost(c.r - MIN), fy.coerceAtMost(c.b - MIN), c.r, c.b)
            Corner.TR -> Crop(c.l, fy.coerceAtMost(c.b - MIN), fx.coerceAtLeast(c.l + MIN), c.b)
            Corner.BL -> Crop(fx.coerceAtMost(c.r - MIN), c.t, c.r, fy.coerceAtLeast(c.t + MIN))
            Corner.BR -> Crop(c.l, c.t, fx.coerceAtLeast(c.l + MIN), fy.coerceAtLeast(c.t + MIN))
        }
    }

    /** The corner within [reach] (same units as x / y, both measured in the shown picture's pixels of size w x h) of the touch, nearest first. */
    fun nearest(c: Crop, x: Float, y: Float, w: Float, h: Float, reach: Float): Corner? =
        Corner.values().map { k ->
            val cx = (if (k == Corner.TL || k == Corner.BL) c.l else c.r) * w
            val cy = (if (k == Corner.TL || k == Corner.TR) c.t else c.b) * h
            k to Math.hypot((cx - x).toDouble(), (cy - y).toDouble())
        }.filter { it.second <= reach }.minByOrNull { it.second }?.first

    /** Where the crop sits in a bitmap of w x h pixels (at least 1 pixel, never outside). */
    fun toPixels(c: Crop, w: Int, h: Int): Px {
        val k = clamp(c)
        val x = Math.round(k.l * w).coerceIn(0, w - 1); val y = Math.round(k.t * h).coerceIn(0, h - 1)
        return Px(x, y, Math.round(k.r * w).coerceIn(x + 1, w) - x, Math.round(k.b * h).coerceIn(y + 1, h) - y)
    }

    fun isPng(name: String) = name.substringAfterLast('.', "").equals("png", true)
    fun extFor(name: String) = if (isPng(name)) "png" else "jpg"

    /** The edited copy's file name: new id, the old name's words, the right ending. */
    fun copyName(old: String, id: String): String {
        val rest = old.substringAfter('-', old).substringBeforeLast('.')
        return "$id-$rest.${extFor(old)}"
    }
}
