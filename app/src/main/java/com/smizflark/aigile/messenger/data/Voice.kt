package com.smizflark.aigile.messenger.data

/** Voice notes in friend chats (AII-331): the plain rules, apart from the recorder and the player. */
object Voice {
    const val TYPE = "audio/mp4"
    /** A note stops by itself and sends at 2 minutes. */
    const val MAX_MS = 120_000L
    /** Shorter than this is a slip of the finger: dropped, not sent. */
    const val MIN_MS = 1_000L

    /** 7_000 -> "0:07", 125_000 -> "2:05" (m:ss). */
    fun label(ms: Long): String {
        val s = (ms.coerceAtLeast(0) / 1000)
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    fun tooShort(ms: Long) = ms < MIN_MS

    fun isAudio(type: String, name: String = "") =
        type.startsWith("audio/") || Regex("(?i)\\.(m4a|mp3|aac|ogg|opus|wav)$").containsMatchIn(name)

    fun isAudio(p: Pic) = isAudio(p.type, p.name.ifEmpty { p.path.substringBefore('?') })

    /** The name the friend sees. */
    fun fileName(timeMs: Long) = "voice-$timeMs.m4a"

    /** The file on the phone: the outbox keeps what follows the first dash as the name (Repo.upload), so this carries a prefix. */
    fun recordingFile(timeMs: Long) = "rec-" + fileName(timeMs)

    /** Playback speeds (AII-378). */
    val SPEEDS = listOf(1f, 1.5f, 2f)

    /** 1 -> 1.5 -> 2 -> 1; anything unknown goes to 1. */
    fun nextSpeed(cur: Float): Float = SPEEDS.indexOf(cur).let { if (it < 0) 1f else SPEEDS[(it + 1) % SPEEDS.size] }

    /** 1f -> "1x", 1.5f -> "1.5x". */
    fun speedLabel(s: Float) = (if (s == s.toInt().toFloat()) s.toInt().toString() else s.toString()) + "x"

    /** A stored "1.5x" or "1.5" back to a speed; unknown or missing -> 1. */
    fun parseSpeed(raw: String?): Float = raw?.trim()?.removeSuffix("x")?.toFloatOrNull()?.takeIf { it in SPEEDS } ?: 1f
}
