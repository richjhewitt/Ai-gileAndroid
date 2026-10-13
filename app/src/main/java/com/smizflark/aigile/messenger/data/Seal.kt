package com.smizflark.aigile.messenger.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Sealed phone link v1 (docs/SEALED.md): every board call is AES-256-GCM sealed end to end
 * with a key derived from the pairing key, so the device key never travels after pairing.
 */
object Seal {
    const val VERSION = "aigile-seal-v1"
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    /** S = HKDF-SHA256(ikm = K, salt = "aigile-seal-v1", info = "phone " + d, 32 bytes). */
    fun deriveKey(k: ByteArray, deviceId: String): ByteArray =
        hkdf(k, VERSION.toByteArray(), "phone $deviceId".toByteArray(), 32)

    /** RFC 5869 HKDF with HMAC-SHA256. */
    fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length in 1..255 * 32)
        val prk = hmac(salt, ikm)
        val out = java.io.ByteArrayOutputStream()
        var t = ByteArray(0)
        var i = 1
        while (out.size() < length) {
            t = hmac(prk, t + info + byteArrayOf(i.toByte()))
            out.write(t); i++
        }
        return out.toByteArray().copyOf(length)
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(key, "HmacSHA256")); doFinal(data) }

    fun aad(dir: String, deviceId: String) = "$VERSION|$dir|$deviceId".toByteArray()

    /** Ciphertext || 16-byte tag. */
    fun encrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, plain: ByteArray): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(aad); doFinal(plain)
        }

    /** Throws (AEADBadTagException) on a bad tag, wrong key or wrong AAD. */
    fun decrypt(key: ByteArray, nonce: ByteArray, aad: ByteArray, sealed: ByteArray): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(aad); doFinal(sealed)
        }

    fun b64(bytes: ByteArray): String = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    fun unb64(text: String): ByteArray = java.util.Base64.getUrlDecoder().decode(text.trim().trimEnd('='))

    fun nonce(): ByteArray = ByteArray(12).also { random.nextBytes(it) }

    /** The request plaintext, keys in the fixed order t, m, p, b with no spaces. */
    fun requestPlain(t: Long, method: String, path: String, body: JsonElement?): String =
        "{\"t\":$t,\"m\":${JsonPrimitive(method)},\"p\":${JsonPrimitive(path)},\"b\":${body ?: JsonNull}}"

    /** The {"n","c"} envelope. */
    fun envelope(nonce: ByteArray, sealed: ByteArray) = "{\"n\":\"${b64(nonce)}\",\"c\":\"${b64(sealed)}\"}"
}

/** One paired phone's sealed link: device id d and its seal key S. */
class SealedLink(val deviceId: String, private val key: ByteArray) {

    constructor(deviceId: String, pairingKey: String) : this(deviceId, Seal.deriveKey(Seal.unb64(pairingKey), deviceId))

    /** The POST /api/phone/sealed body for one inner call. */
    fun request(method: String, path: String, body: JsonElement?, t: Long = System.currentTimeMillis(), nonce: ByteArray = Seal.nonce()): String {
        val plain = Seal.requestPlain(t, method, path, body)
        return Seal.envelope(nonce, Seal.encrypt(key, nonce, Seal.aad("req", deviceId), plain.toByteArray()))
    }

    /** Opens a sealed answer: the inner status and body. Throws on a bad tag. */
    fun response(envelope: String): Inner {
        val e = json.parseToJsonElement(envelope) as JsonObject
        val n = Seal.unb64(e["n"]!!.jsonPrimitive.content)
        val c = Seal.unb64(e["c"]!!.jsonPrimitive.content)
        val plain = String(Seal.decrypt(key, n, Seal.aad("res", deviceId), c))
        val o = json.parseToJsonElement(plain) as JsonObject
        return Inner(o["s"]?.jsonPrimitive?.intOrNull ?: 0, o["b"] ?: JsonNull)
    }

    /** Test/board side: seals an answer the way the board does. */
    fun sealResponse(status: Int, body: JsonElement, nonce: ByteArray = Seal.nonce()): String {
        val plain = "{\"s\":$status,\"b\":$body}"
        return Seal.envelope(nonce, Seal.encrypt(key, nonce, Seal.aad("res", deviceId), plain.toByteArray()))
    }

    /** Test/board side: opens a request body to its plaintext JSON. */
    fun openRequest(envelope: String): String {
        val e = json.parseToJsonElement(envelope) as JsonObject
        return String(Seal.decrypt(key, Seal.unb64(e["n"]!!.jsonPrimitive.content), Seal.aad("req", deviceId),
            Seal.unb64(e["c"]!!.jsonPrimitive.content)))
    }

    class Inner(val status: Int, val body: JsonElement)
}
