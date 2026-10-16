package com.smizflark.aigile.messenger.wear

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/** Runs when the phone changes /chats or /locked, even with the app closed, so the tile stays current (AII-415). */
class WearDataService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        if (events.any { val p = it.dataItem.uri.path; p == WearChats.PATH || p == WearChats.LOCKED }) WearTileService.refresh(applicationContext)
    }
}
