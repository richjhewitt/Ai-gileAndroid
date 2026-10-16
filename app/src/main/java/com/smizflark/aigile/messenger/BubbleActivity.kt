package com.smizflark.aigile.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.PictureHost
import com.smizflark.aigile.messenger.work.CheckWorker

/** Chat heads (AII-381): the floating bubble opens this, one chat only (MainActivity stays a normal singleTop launch). */
class BubbleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as App).repo
        val key = intent.getStringExtra(CheckWorker.EXTRA_CHAT)
        val ref = repo.store.cachedChats()?.firstOrNull { it.key == key }
        if (ref == null) { finish(); return }
        setContent {
            MessengerTheme(repo.store) {
                PictureHost { ChatScreen(repo, ref, onBack = { finish() }) }
            }
        }
    }
}
