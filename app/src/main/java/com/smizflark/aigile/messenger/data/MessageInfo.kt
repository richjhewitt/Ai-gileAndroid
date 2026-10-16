package com.smizflark.aigile.messenger.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** AII-372: Hold a message, Info: which lines the dialog shows. The board keeps no per-message read time, so there is no Read line. */
object MessageInfo {
    enum class Kind { SENT, EDITED }
    data class Line(val kind: Kind, val time: String)

    /** "Fri 16 Oct 2026, 07:41" in the phone's language; always 24h, like the bubbles' times (fullTime in Parts.kt). */
    fun format(iso: String?, zone: ZoneId, locale: Locale): String {
        val t = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
        return DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", locale).format(t.atZone(zone))
    }

    /** Sent first, then Edited when the message was edited; a time that cannot be read is left out. */
    fun lines(sent: String?, edited: String?, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): List<Line> =
        listOf(Kind.SENT to sent, Kind.EDITED to edited).mapNotNull { (k, iso) ->
            format(iso, zone, locale).takeIf { it.isNotEmpty() }?.let { Line(k, it) }
        }
}
