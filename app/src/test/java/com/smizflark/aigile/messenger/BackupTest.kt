package com.smizflark.aigile.messenger

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Settings follow you to a new phone, keys and the board address do not (AII-81). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun src(path: String) = listOf("src/main/$path", "app/src/main/$path").map(::File).first { it.exists() }

    private fun parse(name: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src("res/xml/$name"))

    private fun includes(name: String, tag: String? = null): List<String> {
        val doc = parse(name)
        val scope = if (tag == null) doc.documentElement else doc.getElementsByTagName(tag).item(0) as Element
        val nodes = scope.getElementsByTagName("include")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("path") }
    }

    @Test fun oldPhoneRulesTakeOnlySettings() {
        assertEquals(listOf("settings.xml"), includes("backup_rules.xml"))
    }

    @Test fun newPhoneRulesTakeOnlySettings() {
        assertEquals(listOf("settings.xml"), includes("data_extraction_rules.xml", "cloud-backup"))
        assertEquals(listOf("settings.xml"), includes("data_extraction_rules.xml", "device-transfer"))
    }

    @Test fun pairingFileAndOutboxAreNotIncluded() {
        val all = includes("backup_rules.xml") + includes("data_extraction_rules.xml")
        assertTrue(all.none { it.contains("messenger") })
        assertTrue(all.none { it.contains("outbox") || it.contains("cache") })
    }

    @Test fun manifestPointsAtTheRules() {
        val m = src("AndroidManifest.xml").readText()
        assertTrue(m.contains("@xml/backup_rules") && m.contains("@xml/data_extraction_rules"))
    }

    @Test fun backedUpListHasNoKeys() {
        listOf("url", "key", "device", "owner", "demo", "lock", "pausedUntil", "lastSync").forEach {
            assertFalse(it, it in Store.BACKED_UP)
        }
    }

    @Test fun settingsMoveOutOfThePairingFile() {
        val old = ctx.getSharedPreferences("messenger", Context.MODE_PRIVATE)
        old.edit().putString("look", "black").putString("textSize", "large").putStringSet("pinned", setOf("a"))
            .putStringSet("muted", setOf("b")).putInt("quietFrom", 21).putBoolean("quietOn", false)
            .putString("url", "https://x").putString("key", "sealed").commit()
        Store(ctx)
        val s = ctx.getSharedPreferences(Store.SETTINGS, Context.MODE_PRIVATE)
        assertEquals("black", s.getString("look", null))
        assertEquals("large", s.getString("textSize", null))
        assertEquals(setOf("a"), s.getStringSet("pinned", null))
        assertEquals(setOf("b"), s.getStringSet("muted", null))
        assertEquals(21, s.getInt("quietFrom", 0))
        assertFalse(s.getBoolean("quietOn", true))
        assertFalse(s.contains("url") || s.contains("key"))
        assertFalse(old.contains("look") || old.contains("pinned"))
        assertEquals("https://x", old.getString("url", null))
        assertEquals("sealed", old.getString("key", null))
    }

    @Test fun migrationKeepsNewerValues() {
        val old = ctx.getSharedPreferences("messenger", Context.MODE_PRIVATE)
        val s = ctx.getSharedPreferences(Store.SETTINGS, Context.MODE_PRIVATE)
        s.edit().putString("look", "dark").commit()
        old.edit().putString("look", "light").commit()
        Store(ctx); Store(ctx)
        assertEquals("dark", s.getString("look", null))
    }

    @Test fun restoredAppWithoutKeyIsNotConnected() {
        ctx.getSharedPreferences(Store.SETTINGS, Context.MODE_PRIVATE).edit().putString("look", "dark").commit()
        val st = Store(ctx)
        assertFalse(st.connected)
        assertEquals("dark", st.look)
    }

    @Test fun signOutClearsSettingsToo() {
        val st = Store(ctx); st.look = "dark"; st.signOut()
        assertEquals("system", st.look)
    }

    @Test fun resetKeepsPinsMutesAndSilent() {
        assertEquals(Store.BACKED_UP - setOf("pinned", "muted", "silent"), Store.RESETTABLE)
        assertTrue("look" in Store.RESETTABLE && "quickReplyText" in Store.RESETTABLE)
        listOf("pinned", "muted", "silent", "url", "key", "device", "lock").forEach { assertFalse(it, it in Store.RESETTABLE) }
    }
}
