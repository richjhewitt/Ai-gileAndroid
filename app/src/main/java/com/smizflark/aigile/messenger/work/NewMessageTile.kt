package com.smizflark.aigile.messenger.work

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.smizflark.aigile.messenger.MainActivity
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.ui.Shortcuts

/** AII-400: Quick Settings tile that opens the app straight on New message, the same as the icon menu (AII-53). */
class NewMessageTile : TileService() {
    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.new_message)
        tile.updateTile()
    }

    override fun onClick() {
        val intent = Intent(Shortcuts.ACTION_NEW).setClass(this, MainActivity::class.java)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
