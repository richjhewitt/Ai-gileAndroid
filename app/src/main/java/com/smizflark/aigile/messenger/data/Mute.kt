package com.smizflark.aigile.messenger.data

/** Mute a chat for a while (AII-157): 1 hour, 8 hours or until unmuted. Pure, so it is unit tested: the clock is always passed in. */
object Mute {
    enum class For { HOUR, EIGHT_HOURS, ALWAYS }

    /** When the mute ends (epoch millis), or null for until unmuted. */
    fun until(now: Long, f: For): Long? = when (f) {
        For.HOUR -> now + Pause.HOUR
        For.EIGHT_HOURS -> now + 8 * Pause.HOUR
        For.ALWAYS -> null
    }

    /** The chats still muted at [now]: a timed mute ends by itself. */
    fun active(muted: Set<String>, until: Map<String, Long>, now: Long): Set<String> =
        muted.filterTo(mutableSetOf()) { c -> until[c]?.let { it > now } ?: true }
}
