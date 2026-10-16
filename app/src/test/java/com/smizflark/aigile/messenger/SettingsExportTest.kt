package com.smizflark.aigile.messenger

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.SettingsExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings export file (AII-349): round trip, wrong passphrase, nothing but settings. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsExportTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private fun prefs(name: String) = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)

    @Test fun roundTripRestoresSettings() {
        val a = prefs("a"); a.edit().putString("look", "dark").putBoolean("compact", true).putInt("undoSeconds", 7)
            .putStringSet("pinned", setOf("x", "y")).putString("key", "SECRET").apply()
        val file = SettingsExport.seal(SettingsExport.dump(a), "pw".toCharArray())
        val b = prefs("b")
        assertEquals(4, SettingsExport.load(SettingsExport.open(file, "pw".toCharArray()), b))
        assertEquals("dark", b.getString("look", null)); assertTrue(b.getBoolean("compact", false))
        assertEquals(7, b.getInt("undoSeconds", 0)); assertEquals(setOf("x", "y"), b.getStringSet("pinned", null))
    }

    @Test fun boardKeyNeverGoesIn() {
        val a = prefs("c"); a.edit().putString("key", "SECRET").putString("url", "http://board").putString("look", "dark").apply()
        val text = SettingsExport.dump(a)
        assertFalse(text.contains("SECRET")); assertFalse(text.contains("http://board"))
    }

    @Test fun fileIsNotReadableWithoutPassphrase() {
        val file = SettingsExport.seal("""{"look":{"s":"dark"}}""", "right".toCharArray())
        assertFalse(String(file).contains("dark"))
        try { SettingsExport.open(file, "wrong".toCharArray()); fail() } catch (e: SettingsExport.BadFile) { }
        try { SettingsExport.open("hello".toByteArray(), "right".toCharArray()); fail() } catch (e: SettingsExport.BadFile) { }
    }
}
