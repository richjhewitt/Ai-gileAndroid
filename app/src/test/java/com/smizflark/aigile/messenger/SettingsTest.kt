package com.smizflark.aigile.messenger

import com.smizflark.aigile.messenger.data.Mapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Settings (AII-29): the masked key and when a change moves the phone to another board. */
class SettingsTest {

    /* ---------- phone key, masked ---------- */

    @Test fun showsOnlyLastFour() = assertEquals("••••wxyz", Mapper.maskKey("abcdefwxyz"))

    @Test fun shortKeyFullyHidden() = assertEquals("••••", Mapper.maskKey("abcd"))

    @Test fun noKey() = assertEquals("none", Mapper.maskKey(""))

    /* ---------- another board? ---------- */

    @Test fun sameBoardKeepsCache() = assertFalse(Mapper.boardChanged(false, "https://pc.ts.net", "https://pc.ts.net"))

    @Test fun newAddressClearsCache() = assertTrue(Mapper.boardChanged(false, "https://pc.ts.net", "https://other.ts.net"))

    @Test fun leavingDemoClearsCache() = assertTrue(Mapper.boardChanged(true, "", "https://pc.ts.net"))
}
