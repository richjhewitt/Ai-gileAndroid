package com.smizflark.aigile.messenger.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Settings export and restore (AII-349): the backed-up settings in one file, sealed with a passphrase. Board keys and the address never go in. */
object SettingsExport {
    private const val MAGIC = "AIGM1"
    private const val ROUNDS = 120_000

    /** Wrong passphrase, or not one of our files. */
    class BadFile : Exception()

    private fun key(pass: CharArray, salt: ByteArray) = SecretKeySpec(
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pass, salt, ROUNDS, 256)).encoded, "AES")

    /** The backed-up settings as JSON text, each value tagged with its type. */
    fun dump(p: SharedPreferences): String {
        val o = JSONObject()
        for (k in Store.BACKED_UP) {
            val e = JSONObject()
            when (val v = p.all[k] ?: continue) {
                is String -> e.put("s", v)
                is Boolean -> e.put("b", v)
                is Int -> e.put("i", v)
                is Long -> e.put("l", v)
                is Float -> e.put("f", v.toDouble())
                is Set<*> -> e.put("set", JSONArray(v.filterIsInstance<String>()))
                else -> continue
            }
            o.put(k, e)
        }
        return o.toString()
    }

    /** Puts [text] (from [dump]) back. Only known keys are taken; returns how many. */
    fun load(text: String, p: SharedPreferences): Int {
        val o = runCatching { JSONObject(text) }.getOrElse { throw BadFile() }
        val ed = p.edit(); var n = 0
        for (k in Store.BACKED_UP) {
            val e = o.optJSONObject(k) ?: continue
            when {
                e.has("s") -> ed.putString(k, e.getString("s"))
                e.has("b") -> ed.putBoolean(k, e.getBoolean("b"))
                e.has("i") -> ed.putInt(k, e.getInt("i"))
                e.has("l") -> ed.putLong(k, e.getLong("l"))
                e.has("f") -> ed.putFloat(k, e.getDouble("f").toFloat())
                e.has("set") -> ed.putStringSet(k, e.getJSONArray("set").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() })
                else -> continue
            }
            n++
        }
        ed.apply(); return n
    }

    /** MAGIC, salt, iv and ciphertext in one byte array. */
    fun seal(text: String, pass: CharArray): ByteArray {
        val r = SecureRandom(); val salt = ByteArray(16).also(r::nextBytes); val iv = ByteArray(12).also(r::nextBytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key(pass, salt), GCMParameterSpec(128, iv)) }
        return MAGIC.toByteArray() + salt + iv + c.doFinal(text.toByteArray())
    }

    fun open(file: ByteArray, pass: CharArray): String {
        val m = MAGIC.length
        if (file.size < m + 28 || String(file, 0, m) != MAGIC) throw BadFile()
        return try {
            val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(pass, file.copyOfRange(m, m + 16)), GCMParameterSpec(128, file.copyOfRange(m + 16, m + 28)))
            }
            String(c.doFinal(file, m + 28, file.size - m - 28))
        } catch (e: java.security.GeneralSecurityException) { throw BadFile() }
    }
}
