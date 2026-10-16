package com.smizflark.aigile.messenger.data

/**
 * A chat's own colour (AII-375): tints your bubbles and the chat's top bar in that chat, on this phone only.
 * Stored per chat in the settings prefs (backed up) as "key<TAB>id"; no entry means Default (the app's normal colours).
 */
object ChatColour {
    /** A colour as ARGB: the fill, and the text that sits on it. */
    class Tint(val fill: Long, val text: Long)

    /** One choice: its id and a light and a dark (also used for Black) tint. */
    class Choice(val id: String, val light: Tint, val dark: Tint)

    const val DEFAULT = ""
    private const val SEP = '\t'

    val choices = listOf(
        Choice("blue", Tint(0xFFD0E4FF, 0xFF001D36), Tint(0xFF004A77, 0xFFD0E4FF)),
        Choice("green", Tint(0xFFC8F0CF, 0xFF002106), Tint(0xFF1E5B2B, 0xFFC8F0CF)),
        Choice("teal", Tint(0xFFB4EEE8, 0xFF00201E), Tint(0xFF00504B, 0xFFB4EEE8)),
        Choice("purple", Tint(0xFFE8DDFF, 0xFF22005D), Tint(0xFF4A3A7D, 0xFFE8DDFF)),
        Choice("pink", Tint(0xFFFFD9E3, 0xFF3E001D), Tint(0xFF7A2848, 0xFFFFD9E3)),
        Choice("orange", Tint(0xFFFFDCC2, 0xFF2D1600), Tint(0xFF7A3E00, 0xFFFFDCC2)),
    )

    /** The id stored for [key], or "" for Default (also for an id this version doesn't know). */
    fun of(saved: Set<String>, key: String): String {
        val id = saved.firstOrNull { it.substringBefore(SEP) == key }?.substringAfter(SEP, "") ?: ""
        return if (choices.any { it.id == id }) id else DEFAULT
    }

    /** Saves [id] for [key]; Default removes the entry. */
    fun put(saved: Set<String>, key: String, id: String): Set<String> =
        saved.filterNot { it.substringBefore(SEP) == key }.toSet() + (if (id == DEFAULT) emptySet() else setOf("$key$SEP$id"))

    /** The tint for [id] in light or dark/black; null for Default. */
    fun tint(id: String, dark: Boolean): Tint? = choices.firstOrNull { it.id == id }?.let { if (dark) it.dark else it.light }

    /** WCAG contrast ratio between two ARGB colours (1 to 21). */
    fun contrast(a: Long, b: Long): Double {
        val x = luminance(a); val y = luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    private fun luminance(c: Long): Double {
        fun ch(shift: Int): Double {
            val v = ((c shr shift) and 0xFF) / 255.0
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(16) + 0.7152 * ch(8) + 0.0722 * ch(0)
    }
}
