package com.smizflark.aigile.messenger.data

/**
 * AII-364: how many emoji a message is made of when it is only emoji (whitespace allowed), else 0.
 * 1 to 3 shows big in the chat. Counts ZWJ families, skin tones, flags, variation selectors and keycaps as one each.
 * Grouped by hand (not BreakIterator) so the JVM unit tests and the phone agree.
 */
fun bigEmojiCount(text: String): Int {
    val cps = text.codePoints().toArray()
    var i = 0
    var n = 0
    while (i < cps.size) {
        val c = cps[i]
        if (Character.isWhitespace(c)) { i++; continue }
        val end = cluster(cps, i)
        if (end == i) return 0
        n++
        if (n > 3) return 0
        i = end
    }
    return n
}

private const val ZWJ = 0x200D
private const val VS16 = 0xFE0F
private const val KEYCAP = 0x20E3

private fun isRegional(c: Int) = c in 0x1F1E6..0x1F1FF
private fun isModifier(c: Int) = c in 0x1F3FB..0x1F3FF || c == VS16 || c in 0xE0020..0xE007F

private fun isPictograph(c: Int) = c in 0x1F300..0x1FAFF || c in 0x1F000..0x1F2FF || c in 0x2600..0x27BF ||
    c in 0x2300..0x23FF || c in 0x2B00..0x2BFF || c in 0x2190..0x21FF || c in 0x25A0..0x25FF || c in 0x2900..0x297F ||
    c == 0x203C || c == 0x2049 || c == 0x2122 || c == 0x2139 || c == 0x3030 || c == 0x303D || c == 0x3297 || c == 0x3299

/** End index of the emoji starting at [s], or [s] when there is none. */
private fun cluster(cps: IntArray, s: Int): Int {
    val c = cps[s]
    var i = s + 1
    if (c in 0x30..0x39 || c == 0x23 || c == 0x2A) {   // keycap: 1 + FE0F + 20E3
        if (i < cps.size && cps[i] == VS16) i++
        return if (i < cps.size && cps[i] == KEYCAP) i + 1 else s
    }
    if (isRegional(c)) return if (i < cps.size && isRegional(cps[i])) i + 1 else s
    if (c == 0xA9 || c == 0xAE) return if (i < cps.size && cps[i] == VS16) i + 1 else s
    if (!isPictograph(c)) return s
    while (true) {
        while (i < cps.size && (isModifier(cps[i]) || cps[i] == KEYCAP)) i++
        if (i + 1 < cps.size && cps[i] == ZWJ && (isPictograph(cps[i + 1]) || isRegional(cps[i + 1]))) i += 2 else break
    }
    return i
}
