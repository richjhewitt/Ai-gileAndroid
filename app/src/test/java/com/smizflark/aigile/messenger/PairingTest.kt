package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Mapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Journey 1 (AII-3): the board address as typed, the pairing QR, and the plain-http warning. */
class PairingTest {

    /* ---------- board address ---------- */

    @Test fun addsHttpsWhenNoScheme() =
        assertEquals("https://pc.tail1234.ts.net", Mapper.normaliseUrl("pc.tail1234.ts.net"))

    @Test fun keepsHttpAndPort() =
        assertEquals("http://100.101.102.103:4321", Mapper.normaliseUrl("http://100.101.102.103:4321"))

    @Test fun trimsSpacesAndTrailingSlashes() =
        assertEquals("https://board.example", Mapper.normaliseUrl("  https://board.example//  "))

    @Test fun schemeIsCaseInsensitive() =
        assertEquals("HTTP://pc:4321", Mapper.normaliseUrl("HTTP://pc:4321"))

    @Test fun emptyStaysEmpty() = assertEquals("", Mapper.normaliseUrl("   "))

    /* ---------- pairing QR: aigile://pair?url=...&key=... ---------- */

    @Test fun readsPairingLink() =
        assertEquals("https://pc.ts.net:4321" to "abc123",
            Mapper.parsePairing("aigile://pair?url=https%3A%2F%2Fpc.ts.net%3A4321%2F&key=abc123"))

    @Test fun pairingTidiesTheAddress() =
        assertEquals("https://pc.ts.net" to "k", Mapper.parsePairing("  aigile://pair?url=pc.ts.net&key=k  "))

    @Test fun pairingAcceptsTokenForKey() =
        assertEquals("https://pc" to "t1", Mapper.parsePairing("aigile://pair?url=pc&token=t1"))

    @Test fun pairingParamOrderDoesNotMatter() =
        assertEquals("https://pc" to "k", Mapper.parsePairing("AIGILE://pair?key=k&url=pc"))

    @Test fun notAPairingLink() {
        assertNull(Mapper.parsePairing("https://pc.ts.net/?key=k"))
        assertNull(Mapper.parsePairing("hello"))
        assertNull(Mapper.parsePairing(""))
    }

    @Test fun pairingNeedsBothParts() {
        assertNull(Mapper.parsePairing("aigile://pair?url=pc"))
        assertNull(Mapper.parsePairing("aigile://pair?key=k"))
        assertNull(Mapper.parsePairing("aigile://pair"))
    }

    /* ---------- plain http warning ---------- */

    @Test fun httpsIsNeverInsecure() = assertFalse(Mapper.insecure("https://anything.example"))

    @Test fun httpToTailscaleIsFine() {
        assertFalse(Mapper.insecure("http://100.64.0.1:4321"))
        assertFalse(Mapper.insecure("http://100.127.255.254"))
        assertFalse(Mapper.insecure("http://pc.tail1234.ts.net:4321/"))
    }

    @Test fun httpToThisPcOrEmulatorIsFine() {
        assertFalse(Mapper.insecure("http://localhost:4321"))
        assertFalse(Mapper.insecure("http://127.0.0.1:4321"))
        assertFalse(Mapper.insecure("http://10.0.2.2:4321"))
    }

    @Test fun httpElsewhereIsInsecure() {
        assertTrue(Mapper.insecure("http://192.168.1.20:4321"))
        assertTrue(Mapper.insecure("http://100.63.0.1"))
        assertTrue(Mapper.insecure("http://100.128.0.1"))
        assertTrue(Mapper.insecure("HTTP://board.example"))
    }
}
