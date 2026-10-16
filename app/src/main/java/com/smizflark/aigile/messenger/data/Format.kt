package com.smizflark.aigile.messenger.data

/** Bold, italic and code buttons for the message box (AII-374): wrap or unwrap the selection in the markers the bubbles show. */
object Format {
    const val BOLD = "**"
    const val ITALIC = "*"   // the board's italic: single asterisks, so ** (bold) must not count as one
    const val CODE = "`"

    /** The new text and the selection (start to end) to keep. */
    data class Result(val text: String, val start: Int, val end: Int)

    /**
     * Toggles [marker] around the selection [start] to [end]. An empty selection does nothing. A selection already
     * wrapped (inside it, or with the markers just outside it) is unwrapped. Each line is wrapped on its own,
     * because the bubbles' markers do not reach across a line break.
     */
    fun toggle(text: String, start: Int, end: Int, marker: String): Result {
        val a = minOf(start, end).coerceIn(0, text.length)
        val b = maxOf(start, end).coerceIn(0, text.length)
        if (a == b || text.substring(a, b).isBlank()) return Result(text, a, b)
        val m = marker.length
        val sel = text.substring(a, b)
        // A single * is italic only when it is not half of a ** (bold).
        fun lone(i: Int) = marker != "*" || text.getOrNull(i) != '*'
        if (!sel.contains('\n')) {
            if (sel.length > 2 * m && sel.startsWith(marker) && sel.endsWith(marker) && (marker != "*" || (sel[1] != '*' && sel[sel.length - 2] != '*')))
                return Result(text.substring(0, a) + sel.substring(m, sel.length - m) + text.substring(b), a, b - 2 * m)
            if (a >= m && text.startsWith(marker, a - m) && text.startsWith(marker, b) && lone(a - m - 1) && lone(b + m))
                return Result(text.substring(0, a - m) + sel + text.substring(b + m), a - m, b - m)
            return Result(text.substring(0, a) + marker + sel + marker + text.substring(b), a + m, b + m)
        }
        val lines = sel.split('\n')
        fun wrapped(l: String) = l.length > 2 * m && l.startsWith(marker) && l.endsWith(marker) && (marker != "*" || (l[1] != '*' && l[l.length - 2] != '*'))
        val unwrap = lines.filter { it.isNotBlank() }.all(::wrapped)
        val out = lines.joinToString("\n") { l ->
            when {
                l.isBlank() -> l
                unwrap -> l.substring(m, l.length - m)
                else -> marker + l + marker
            }
        }
        return Result(text.substring(0, a) + out + text.substring(b), a, a + out.length)
    }
}
