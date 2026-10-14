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

    /** The clock time a timed mute ends, in the phone's own style (AII-160, AII-165). */
    fun endTime(end: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault(), locale: java.util.Locale = java.util.Locale.getDefault()): String =
        java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).withLocale(locale)
            .format(java.time.Instant.ofEpochMilli(end).atZone(zone))
}
