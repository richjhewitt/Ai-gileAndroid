package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFindTest {
    private val lock = listOf("Lock with fingerprint", "Asks for your fingerprint, face or phone PIN when you open the app")
    private val lockNl = listOf("Vergrendelen met vingerafdruk", "Vraagt om je vingerafdruk, gezicht of pincode")

    @Test fun emptyShowsEverything() {
        assertTrue(SettingsFind.matches("", lock))
        assertTrue(SettingsFind.matches("   ", lock))
        assertFalse(SettingsFind.active(" "))
    }
    @Test fun englishCaseInsensitive() {
        assertTrue(SettingsFind.matches("LOCK", lock))
        assertTrue(SettingsFind.matches("finger", lock))
        assertFalse(SettingsFind.matches("sound", lock))
        assertFalse(SettingsFind.matches("lock", listOf("Block screenshots")))
    }
    @Test fun multiWordEveryWordMustMatch() {
        assertTrue(SettingsFind.matches("face pin", lock))
        assertFalse(SettingsFind.matches("face sound", lock))
    }
    @Test fun dutchAndAccents() {
        assertTrue(SettingsFind.matches("vingerafdruk", lockNl))
        assertTrue(SettingsFind.matches("Geluid", listOf("Geluid en trillen")))
        assertTrue(SettingsFind.matches("meldingen", listOf("Meldingen bij nieuwe berichten")))
        assertTrue(SettingsFind.matches("een", listOf("Eén melding per keer")))
        assertTrue(SettingsFind.matches("café", listOf("Cafe")))
        assertEquals("een zoek", SettingsFind.normalise("Eén, ZOEK!"))
    }
}
