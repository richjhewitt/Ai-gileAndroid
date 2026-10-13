package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.BoardError
import com.smizflark.aigile.messenger.data.BoardSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

/** Journey 1 (AII-3): signing in against a throwaway board on its own port. */
class SignInTest {
    private lateinit var board: ServerSocket
    private lateinit var url: String

    /** A tiny board: GET /api/meta answers only to "Bearer good-key". */
    @Before fun startBoard() {
        board = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
        url = "http://127.0.0.1:${board.localPort}"
        thread(isDaemon = true) {
            while (!board.isClosed) {
                val s = runCatching { board.accept() }.getOrNull() ?: break
                s.use {
                    val head = it.getInputStream().bufferedReader().let { r ->
                        generateSequence { r.readLine() }.takeWhile { l -> l.isNotEmpty() }.toList()
                    }
                    val ok = head.any { l -> l.equals("Authorization: Bearer good-key", ignoreCase = true) }
                    val body = if (ok) """{"owner":"Rich","extra":1}""" else """{"error":"bad key"}"""
                    val status = if (ok) "200 OK" else "401 Unauthorized"
                    it.getOutputStream().write(
                        ("HTTP/1.1 $status\r\nContent-Type: application/json\r\nContent-Length: ${body.length}\r\n" +
                            "Connection: close\r\n\r\n$body").toByteArray()
                    )
                }
            }
        }
    }

    @After fun stopBoard() = board.close()

    @Test fun signsInWithTheRightKey() = runTest {
        assertEquals("Rich", BoardSource(url, "good-key").owner())
    }

    @Test fun wrongKeyGivesAPlainMessage() = runTest {
        try {
            BoardSource(url, "wrong").owner()
            fail("signed in with a wrong key")
        } catch (e: BoardError) {
            assertEquals(401, e.code)
            assertTrue(e.message!!.contains("didn't accept"))
        }
    }

    @Test fun noBoardGivesAPlainMessage() = runTest {
        board.close()
        try {
            BoardSource(url, "good-key").owner()
            fail("signed in with no board")
        } catch (e: BoardError) {
            assertTrue(e.message!!.startsWith("Can't reach the board"))
        }
    }
}
