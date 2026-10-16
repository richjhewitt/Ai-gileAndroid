package com.smizflark.aigile.messenger.data

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

/**
 * End-to-end sealing for direct messages (AII-325, docs/DM.md). Each device holds a P-256 key pair;
 * two people derive one shared key by ECDH, then every message is AES-256-GCM sealed with [Seal]'s
 * primitives, so the board only ever relays ciphertext. P-256 because every Android from API 26 has it.
 */
object DmCrypto {
    const val VERSION = "aigile-dm-v1"

    fun newKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    fun publicB64(pair: KeyPair): String = Seal.b64(pair.public.encoded)

    private fun publicKey(b64: String): PublicKey =
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Seal.unb64(b64)))

    /** Short id of a public key, shown next to a person so a swapped key is noticed. */
    fun fingerprint(publicB64: String): String =
        MessageDigest.getInstance("SHA-256").digest(Seal.unb64(publicB64)).take(8)
            .joinToString("") { "%02x".format(it) }

    /** The same 30 digits on both phones (groups of 5): read them out or compare to confirm nobody is in the middle. */
    fun safetyNumber(myPublicB64: String, theirPublicB64: String): String {
        val keys = listOf(myPublicB64, theirPublicB64).sorted()
        val digest = MessageDigest.getInstance("SHA-256").digest((VERSION + "|" + keys.joinToString("|")).toByteArray())
        val digits = digest.take(15).joinToString("") { "%02d".format((it.toInt() and 0xff) % 100) }
        return digits.chunked(5).joinToString(" ")
    }

    /** One 32-byte key per pair of devices; the same from either side. */
    fun sharedKey(mine: KeyPair, theirPublicB64: String): ByteArray {
        val secret = KeyAgreement.getInstance("ECDH").run {
            init(mine.private); doPhase(publicKey(theirPublicB64), true); generateSecret()
        }
        val pair = listOf(publicB64(mine), theirPublicB64).sorted().joinToString("|")
        return Seal.hkdf(secret, VERSION.toByteArray(), "dm $pair".toByteArray(), 32)
    }

    private fun aad(chatId: String, senderFingerprint: String) = "$VERSION|$chatId|$senderFingerprint".toByteArray()

    /** "nonce.ciphertext" as url-safe text, ready to store on the board. */
    fun seal(key: ByteArray, chatId: String, senderFingerprint: String, text: String, nonce: ByteArray = Seal.nonce()): String {
        val sealed = Seal.encrypt(key, nonce, aad(chatId, senderFingerprint), text.toByteArray())
        return Seal.b64(nonce) + "." + Seal.b64(sealed)
    }

    /** Throws on a wrong key, a changed message, or a message moved to another chat or sender. */
    fun open(key: ByteArray, chatId: String, senderFingerprint: String, sealed: String): String {
        val (n, c) = sealed.split('.', limit = 2).also { require(it.size == 2) { "not a sealed DM" } }
        return String(Seal.decrypt(key, Seal.unb64(n), aad(chatId, senderFingerprint), Seal.unb64(c)))
    }
}
