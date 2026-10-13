package com.smizflark.aigile.messenger.connect

import com.smizflark.aigile.messenger.data.BoardError
import com.smizflark.aigile.messenger.data.BoardSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

class PairingTest {

    /* ---------- parse: the QR code / pasted link ---------- */

    @Test fun parsesPairingLink() {
        val p = PairLink.parse("aigile://pair?url=https%3A%2F%2Fpc.tail.ts.net%2F&key=abc123")
        assertEquals(Pairing("https://pc.tail.ts.net", "abc123"), p)
    }

    @Test fun rejectsOtherCodes() {
        assertNull(PairLink.parse("https://example.com"))
        assertNull(PairLink.parse("aigile://pair?url=https%3A%2F%2Fpc"))
        assertNull(PairLink.parse("aigile://other?url=https%3A%2F%2Fpc&key=k"))
        assertNull(PairLink.parse("not a link at all"))
    }

    /* ---------- address: what was typed ---------- */

    @Test fun tidiesTypedAddress() {
        assertEquals("https://pc.tail.ts.net", PairLink.address("  pc.tail.ts.net/ "))
        assertEquals("http://192.168.1.5:4321", PairLink.address("http://192.168.1.5:4321"))
        assertNull(PairLink.address(""))
        assertNull(PairLink.address("ftp://pc"))
    }

    /* ---------- check: signs in against a small stand-in board on this PC ---------- */

    private lateinit var server: ServerSocket
    private val goodKey = "phone-key-1"

    /** Answers GET /api/meta: 200 with the owner for the right key, 401 otherwise. */
    @Before fun startBoard() {
        server = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val s = runCatching { server.accept() }.getOrNull() ?: break
                s.use {
                    val head = it.getInputStream().bufferedReader().let { r -> generateSequence { r.readLine() }.takeWhile { l -> l.isNotEmpty() }.toList() }
                    val ok = head.any { l -> l.equals("Authorization: Bearer $goodKey", ignoreCase = true) }
                    val body = if (ok) """{"owner":"Rich"}""" else """{"error":"bad key"}"""
                    val status = if (ok) "200 OK" else "401 Unauthorized"
                    it.getOutputStream().write("HTTP/1.1 $status\r\nContent-Type: application/json\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body".toByteArray())
                }
            }
        }
    }

    @After fun stopBoard() = server.close()

    private fun base() = "http://127.0.0.1:${server.localPort}"

    @Test fun signsInWithRightKey() = runTest {
        assertEquals("Rich", PairLink.check(Pairing(base(), goodKey), BoardSource(base(), goodKey)))
    }

    @Test fun wrongKeyGivesPlainMessage() = runTest {
        try {
            PairLink.check(Pairing(base(), "nope"), BoardSource(base(), "nope"))
            fail("should not sign in")
        } catch (e: BoardError) {
            assertTrue(e.message!!.contains("didn't accept this phone's key"))
        }
    }
}
