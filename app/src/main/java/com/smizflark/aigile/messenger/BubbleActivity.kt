package com.smizflark.aigile.messenger

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.Lock
import com.smizflark.aigile.messenger.ui.LockedScreen
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.PictureHost
import com.smizflark.aigile.messenger.work.CheckWorker

/**
 * Chat heads (AII-381): the floating bubble opens this, one chat only (MainActivity stays a normal singleTop launch).
 * It follows the App lock the same way MainActivity does (Lock.shouldLock / Lock.ask, lock-after time, no recents preview).
 */
class BubbleActivity : FragmentActivity() {
    private var locked by mutableStateOf(false)
    private var unlocked = false
    private var leftAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { b ->
            unlocked = b.getBoolean("unlocked")
            leftAt = b.getLong("leftAt", -1).takeIf { it >= 0 }
        }
        enableEdgeToEdge()
        val repo = (application as App).repo
        val key = intent.getStringExtra(CheckWorker.EXTRA_CHAT)
        val ref = repo.store.cachedChats()?.firstOrNull { it.key == key }
        if (ref == null) { finish(); return }
        // Locked from the first frame, so the chat never shows before the lock is passed.
        locked = Lock.shouldLock(repo.store.lock, unlocked, leftAt, SystemClock.elapsedRealtime(), repo.store.lockAfter)
        setContent {
            MessengerTheme(repo.store) {
                if (locked) {
                    LockedScreen(onUnlock = { unlock() })
                    androidx.compose.runtime.LaunchedEffect(Unit) { unlock() }
                } else PictureHost { ChatScreen(repo, ref, onBack = { finish() }) }
            }
        }
    }

    private fun unlock() = Lock.ask(this) { unlocked = true; locked = false }

    override fun onStart() {
        super.onStart()
        com.smizflark.aigile.messenger.data.Secure.apply(window, (application as App).repo.store.blockShots)
        val store = (application as App).repo.store
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!store.lock)
        if (Lock.shouldLock(store.lock, unlocked, leftAt, SystemClock.elapsedRealtime(), store.lockAfter)) { unlocked = false; locked = true }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) leftAt = SystemClock.elapsedRealtime()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("unlocked", unlocked)
        leftAt?.let { outState.putLong("leftAt", it) }
    }
}
