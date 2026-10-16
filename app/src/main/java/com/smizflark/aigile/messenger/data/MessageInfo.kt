package com.smizflark.aigile.messenger.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** AII-372: Hold a message, Info: which lines the dialog shows. The board keeps no per-message read time, so there is no Read line. */
object MessageInfo {
    enum class Kind { SENT, EDITED }
    data class Line(val kind: Kind, val time: String)

    /** "Fri 16 Oct 2026, 07:41" in the phone's language and 12/24h setting. */
    fun format(iso: String?, zone: ZoneId, locale: Locale, h24: Boolean): String {
        val t = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
        return DateTimeFormatter.ofPattern(if (h24) "EEE d MMM yyyy, HH:mm" else "EEE d MMM yyyy, h:mm a", locale).format(t.atZone(zone))
    }

    /** Sent first, then Edited when the message was edited; a time that cannot be read is left out. */
    fun lines(sent: String?, edited: String?, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault(), h24: Boolean = true): List<Line> =
        listOf(Kind.SENT to sent, Kind.EDITED to edited).mapNotNull { (k, iso) ->
            format(iso, zone, locale, h24).takeIf { it.isNotEmpty() }?.let { Line(k, it) }
        }
}
