package com.smizflark.aigile.messenger.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Remembers the paired board. The phone key is sealed with a key held in the Android Keystore,
 * so the saved file alone can't be used to reach the board.
 */
class KeyVault(context: Context) {
    private val prefs = context.getSharedPreferences("pairing", Context.MODE_PRIVATE)

    /** The saved board, or null when the phone isn't paired (or the sealed key can't be opened any more). */
    fun load(): Pairing? {
        val url = prefs.getString("url", null) ?: return null
        val sealed = prefs.getString("key", null) ?: return null
        val key = runCatching { open(sealed) }.getOrNull() ?: return null
        return Pairing(url, key)
    }

    fun save(p: Pairing) {
        prefs.edit().putString("url", p.url).putString("key", seal(p.key)).putBoolean("demo", false).apply()
    }

    /** Demo mode: no board, nothing saved but the choice. */
    var demo: Boolean
        get() = prefs.getBoolean("demo", false)
        set(v) { prefs.edit().putBoolean("demo", v).apply() }

    /** Forget the board (sign out). The address is kept to save typing it again. */
    fun forget() {
        prefs.edit().remove("key").putBoolean("demo", false).apply()
    }

    /** The last board address used, to fill in the connect screen. */
    fun lastAddress(): String? = prefs.getString("url", null)

    private fun secret(): SecretKey {
        val ks = KeyStore.getInstance(STORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun seal(plain: String): String {
        val c = Cipher.getInstance(CIPHER).apply { init(Cipher.ENCRYPT_MODE, secret()) }
        val out = c.iv + c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun open(sealed: String): String {
        val all = Base64.decode(sealed, Base64.NO_WRAP)
        val c = Cipher.getInstance(CIPHER).apply {
            init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, all, 0, IV))
        }
        return String(c.doFinal(all, IV, all.size - IV), Charsets.UTF_8)
    }

    private companion object {
        const val STORE = "AndroidKeyStore"
        const val ALIAS = "aigile-board-key"
        const val CIPHER = "AES/GCM/NoPadding"
        const val IV = 12
    }
}
