package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.MessageInfo
import com.smizflark.aigile.messenger.data.MessageInfo.Kind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class MessageInfoTest {
    private val z = ZoneId.of("Europe/London")
    @Test fun sentOnly() {
        val l = MessageInfo.lines("2026-10-16T06:41:00Z", null, z, Locale.UK)
        assertEquals(listOf(MessageInfo.Line(Kind.SENT, "Fri 16 Oct 2026, 07:41")), l)
    }
    @Test fun editedAddsALine() {
        val l = MessageInfo.lines("2026-10-16T06:41:00Z", "2026-10-16T07:05:00Z", z, Locale.UK)
        assertEquals(listOf(Kind.SENT, Kind.EDITED), l.map { it.kind })
        assertEquals("Fri 16 Oct 2026, 08:05", l[1].time)
    }
    @Test fun alwaysTwentyFourHourLikeTheBubbles() {
        assertEquals("Fri 16 Oct 2026, 15:41", MessageInfo.format("2026-10-16T14:41:00Z", z, Locale.UK))
    }
    @Test fun dutch() {
        assertEquals("vr 16 okt 2026, 07:41", MessageInfo.format("2026-10-16T06:41:00Z", z, Locale("nl", "NL")).replace(".", ""))
    }
    @Test fun badTimeLeftOut() {
        assertEquals(emptyList<MessageInfo.Line>(), MessageInfo.lines("", "x", z, Locale.UK))
    }
}
