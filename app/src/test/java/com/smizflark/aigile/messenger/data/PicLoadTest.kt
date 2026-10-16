package com.smizflark.aigile.messenger.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PicLoadTest {
    private fun load(setting: String = PicLoad.WIFI, metered: Boolean = true, cached: Boolean = false, mine: Boolean = false, tapped: Boolean = false) =
        PicLoad.shouldLoad(setting, metered, cached, mine, tapped)

    @Test fun alwaysLoads() = assertTrue(load(setting = PicLoad.ALWAYS))
    @Test fun wifiOnlyOnWifiLoads() = assertTrue(load(metered = false))
    @Test fun wifiOnlyOnMobileWaits() = assertFalse(load())
    @Test fun cachedShows() = assertTrue(load(cached = true))
    @Test fun mineShows() = assertTrue(load(mine = true))
    @Test fun tappedLoads() = assertTrue(load(tapped = true))
    @Test fun unknownSettingLoads() = assertTrue(load(setting = "x"))
}
