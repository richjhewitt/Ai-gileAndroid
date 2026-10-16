package com.smizflark.aigile.messenger.work

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Pause
import com.smizflark.aigile.messenger.data.Store
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** AII-367: pure logic of the Pause alerts tile. */
object PauseTileLogic {
    /** The new pausedUntil after a tap: alerts back on when paused, else paused for 1 hour. */
    fun tapped(now: Long, until: Long): Long = if (Pause.paused(now, until)) 0L else Pause.oneHour(now)

    /** The clock time the pause ends, like "07:15" (phone's own time style), for the tile subtitle. */
    fun endText(until: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(until).atZone(zone))
}

/** Quick Settings tile: tap to pause alerts for 1 hour, tap again to turn them back on (AII-367). */
class PauseTile : TileService() {
    override fun onStartListening() = refresh()

    override fun onClick() {
        val store = Store(this)
        store.pausedUntil = PauseTileLogic.tapped(Now.millis(), store.pausedUntil)
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val until = Store(this).pausedUntil
        val paused = Pause.paused(Now.millis(), until)
        tile.state = if (paused) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.pause_alerts)
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (paused) getString(R.string.tile_until, PauseTileLogic.endText(until)) else null
        }
        tile.updateTile()
    }
}
