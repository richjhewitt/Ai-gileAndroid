package com.smizflark.aigile.messenger.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import javax.crypto.AEADBadTagException

/** The fixed vectors from docs/SEALED.md; the board repo checks the same ones. */
class SealTest {
    private val k = ByteArray(32) { it.toByte() }
    private val d = "a1b2c3d4e5f6"
    private val n = ByteArray(12) { it.toByte() }
    private val t = 1700000000000L

    private val kB64 = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8"
    private val sB64 = "b8A3-7OMQrGQV8qPfAkIm40ylw_x7R7MS7xdpHM0ups"
    private val reqPlain = """{"t":1700000000000,"m":"GET","p":"/api/chats","b":null}"""
    private val reqBody = """{"n":"AAECAwQFBgcICQoL","c":"C-rMPhBge0WYfeqf1OqyeWA2CmLnmtGeNQtNEWbw8REEkdGFukqxDT2RcAu8Ik1K7ChJ1XsR44LY5CDa2EY7npuTGoY_kxQ"}"""
    private val resPlain = """{"s":200,"b":{"ok":true}}"""
    private val resBody = """{"n":"AAECAwQFBgcICQoL","c":"C-rLPhBjfEWEb7iN3qGgJjskHDT4zY7BD6Bp_BG4--LDx5YkHkk7gEs"}"""

    private val link = SealedLink(d, kB64)

    @Test fun keyVector() {
        assertEquals(kB64, Seal.b64(k))
        assertEquals(sB64, Seal.b64(Seal.deriveKey(k, d)))
    }

    @Test fun hkdfRfc5869Case1() {
        val okm = Seal.hkdf(ByteArray(22) { 0x0b }, hex("000102030405060708090a0b0c"), hex("f0f1f2f3f4f5f6f7f8f9"), 42)
        assertEquals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865", okm.joinToString("") { "%02x".format(it) })
    }

    @Test fun requestVector() {
        assertEquals(reqPlain, Seal.requestPlain(t, "GET", "/api/chats", null))
        assertEquals(reqBody, link.request("GET", "/api/chats", null, t = t, nonce = n))
        assertEquals(reqPlain, link.openRequest(reqBody))
    }

    @Test fun responseVector() {
        assertEquals(resBody, link.sealResponse(200, json.parseToJsonElement("""{"ok":true}"""), nonce = n))
        val inner = link.response(resBody)
        assertEquals(200, inner.status)
        assertEquals("""{"ok":true}""", inner.body.toString())
    }

    @Test fun badTagFails() {
        val sealed = Seal.unb64(cOf(resBody))
        sealed[sealed.size - 1] = (sealed[sealed.size - 1].toInt() xor 1).toByte()
        expectBadTag { Seal.decrypt(Seal.deriveKey(k, d), n, Seal.aad("res", d), sealed) }
        val flipped = Seal.unb64(cOf(resBody)).also { it[0] = (it[0].toInt() xor 1).toByte() }
        expectBadTag { link.response(Seal.envelope(n, flipped)) }
    }

    @Test fun wrongAadFails() {
        val sealed = Seal.unb64(cOf(reqBody))
        val s = Seal.deriveKey(k, d)
        expectBadTag { Seal.decrypt(s, n, Seal.aad("res", d), sealed) }          // request opened as a response
        expectBadTag { Seal.decrypt(s, n, Seal.aad("req", "a1b2c3d4e5f7"), sealed) } // another device's AAD
        expectBadTag { SealedLink(d, kB64).response(reqBody) }
    }

    @Test fun pairingForms() {
        assertEquals("https://pc.ts.net" to "$d:$kB64", Mapper.parsePairing("aigile://pair?url=https%3A%2F%2Fpc.ts.net&id=$d&key=$kB64"))
        assertEquals("https://pc.ts.net" to "oldkey", Mapper.parsePairing("aigile://pair?url=https://pc.ts.net&key=oldkey"))
        assertEquals(d to kB64, Mapper.splitCredential("$d:$kB64"))
        assertEquals("" to "oldkey", Mapper.splitCredential("oldkey"))
        assertNull(Mapper.parsePairing("https://example.com"))
    }

    /** The whole BoardSource path against a fake sealed board: no Authorization header, only the device id in the clear. */
    @Test fun boardSourceSealsCalls() = runTest {
        val seen = mutableListOf<String>()
        val board = Interceptor { chain ->
            val r = chain.request()
            assertEquals("/api/phone/sealed", r.url.encodedPath)
            assertNull(r.header("Authorization"))
            assertEquals(d, r.header("X-Aigile-Device"))
            val body = Buffer().also { r.body!!.writeTo(it) }.readUtf8()
            val inner = json.parseToJsonElement(link.openRequest(body)) as JsonObject
            assertTrue(Math.abs(System.currentTimeMillis() - inner["t"]!!.jsonPrimitive.content.toLong()) < 120_000)
            val p = inner["p"]!!.jsonPrimitive.content
            seen += inner["m"]!!.jsonPrimitive.content + " " + p
            val answer = when (p) {
                "/api/meta" -> link.sealResponse(200, json.parseToJsonElement("""{"owner":"Rich","sealed":1}"""))
                else -> link.sealResponse(404, json.parseToJsonElement("""{"error":"nope"}"""))
            }
            Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(answer.toResponseBody("application/json".toMediaType())).build()
        }
        val src = BoardSource("https://pc.ts.net", kB64, OkHttpClient.Builder().addInterceptor(board).build(), deviceId = d)
        assertEquals("Rich", src.owner())
        assertTrue(src.sealed())
        try { src.conversation(ChatRef(kind = Kind.LEAD, id = "x", name = "x", subtitle = "", emoji = "")); fail() } catch (e: BoardError) { assertEquals(404, e.code) }
        assertEquals(listOf("GET /api/meta", "GET /api/meta", "GET /api/chats/x"), seen)
    }

    /** AII-314: the board's newest build and its APK over the sealed link (the file comes back as {ct, data: base64}). */
    @Test fun updateComesOverTheSealedLink() = runTest {
        val apk = ByteArray(3000) { (it * 7).toByte() }
        val board = Interceptor { chain ->
            val r = chain.request()
            val inner = json.parseToJsonElement(link.openRequest(Buffer().also { r.body!!.writeTo(it) }.readUtf8())) as JsonObject
            val answer = when (inner["p"]!!.jsonPrimitive.content) {
                "/api/meta" -> link.sealResponse(200, json.parseToJsonElement("""{"owner":"Rich","sealed":1}"""))
                "/api/messenger/latest" -> link.sealResponse(200, json.parseToJsonElement("""{"build":400,"version":"0.1.0","url":"/api/messenger/apk"}"""))
                "/api/messenger/apk" -> link.sealResponse(200, json.parseToJsonElement("""{"ct":"application/octet-stream","data":"${java.util.Base64.getEncoder().encodeToString(apk)}"}"""))
                else -> link.sealResponse(404, json.parseToJsonElement("""{"error":"nope"}"""))
            }
            Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(answer.toResponseBody("application/json".toMediaType())).build()
        }
        val src = BoardSource("https://pc.ts.net", kB64, OkHttpClient.Builder().addInterceptor(board).build(), deviceId = d)
        assertEquals(Release(400, "/api/messenger/apk", board = true), src.latest())
        assertTrue(apk.contentEquals(src.fileBytes("/api/messenger/apk")))
    }

    /** AII-314: an older board (no route, 404) has no latest build; a plain link gets the APK bytes as they are. */
    @Test fun updateOverAPlainLink() = runTest {
        val apk = ByteArray(2000) { (it * 3).toByte() }
        var hasRoute = false
        val board = Interceptor { chain ->
            val r = chain.request()
            assertEquals("Bearer oldkey", r.header("Authorization"))
            val (code, body) = when {
                r.url.encodedPath == "/api/messenger/apk" && hasRoute -> 200 to apk.toResponseBody("application/octet-stream".toMediaType())
                else -> 404 to """{"error":"no such endpoint"}""".toResponseBody("application/json".toMediaType())
            }
            Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(code).message("x").body(body).build()
        }
        val src = BoardSource("https://pc.ts.net", "oldkey", OkHttpClient.Builder().addInterceptor(board).build())
        assertNull(src.latest())
        hasRoute = true
        assertTrue(apk.contentEquals(src.fileBytes("/api/messenger/apk")))
    }

    private fun cOf(envelope: String) = (json.parseToJsonElement(envelope) as JsonObject)["c"]!!.jsonPrimitive.content
    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun expectBadTag(block: () -> Unit) {
        try { block(); fail("opened with a bad tag or AAD") } catch (e: AEADBadTagException) { }
    }
}
