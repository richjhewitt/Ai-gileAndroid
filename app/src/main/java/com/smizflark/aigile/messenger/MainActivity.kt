package com.smizflark.aigile.messenger

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Share
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.ChatsScreen
import com.smizflark.aigile.messenger.ui.ConnectScreen
import com.smizflark.aigile.messenger.ui.Lock
import com.smizflark.aigile.messenger.ui.LockedScreen
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.NewMessageScreen
import com.smizflark.aigile.messenger.ui.PictureHost
import com.smizflark.aigile.messenger.ui.SearchScreen
import com.smizflark.aigile.messenger.ui.SettingsScreen
import com.smizflark.aigile.messenger.ui.StarredScreen
import com.smizflark.aigile.messenger.ui.Shortcuts
import com.smizflark.aigile.messenger.work.CheckWorker

class MainActivity : FragmentActivity() {
    /** A pairing link (aigile://pair?...) or a chat to open from an alert. */
    private var pairing by mutableStateOf<Pair<String, String>?>(null)
    private var openChat by mutableStateOf<String?>(null)
    /** Text shared in from another app, waiting to be sent from New message. */
    private var shared by mutableStateOf<String?>(null)
    /** New message from the app icon menu (AII-53). */
    private var openNew by mutableStateOf(false)
    /** App lock (AII-63): covers the app until unlocked; leftAt is when it last went to the background. */
    private var locked by mutableStateOf(false)
    private var unlocked = false
    private var leftAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Turning the phone keeps it unlocked; leftAt is on the boot clock, so a killed app still locks after the grace time.
        savedInstanceState?.let { b ->
            unlocked = b.getBoolean("unlocked")
            leftAt = b.getLong("leftAt", -1).takeIf { it >= 0 }
        }
        enableEdgeToEdge()
        read(intent)
        val repo = (application as App).repo
        setContent {
            MessengerTheme(repo.store) {
                val nav = rememberNavController()
                val start = if (repo.store.connected) "chats" else "connect"
                PictureHost { NavHost(nav, startDestination = start) {
                    composable("connect") {
                        ConnectScreen(repo, pairing) {
                            pairing = null
                            if (repo.store.alerts && !repo.store.demo) CheckWorker.schedule(this@MainActivity)
                            nav.navigate("chats") { popUpTo("connect") { inclusive = true } }
                            if (shared != null) nav.navigate("new")
                        }
                    }
                    composable("chats") {
                        ChatsScreen(repo, onOpen = { nav.openChat(it.key) }, onNew = { nav.navigate("new") },
                            onSettings = { nav.navigate("settings") }, onSearch = { nav.navigate("search") }, onStarred = { nav.navigate("starred") })
                    }
                    composable("starred") {   // AII-140: every starred message; tap one, its chat opens at it
                        StarredScreen(repo, onOpen = { ref, msg -> nav.openChat(ref.key, msg) }, onBack = { nav.popBackStack() })
                    }
                    composable("search") {
                        SearchScreen(repo, onOpen = { ref, msg -> nav.openChat(ref.key, msg) }, onBack = { nav.popBackStack() })
                    }
                    // A chat, or one card of it (?card=AIG-12, AII-28).
                    composable("chat/{key}?card={card}&msg={msg}", arguments = listOf(navArgument("card") { type = NavType.StringType; nullable = true },
                        navArgument("msg") { type = NavType.StringType; nullable = true })) { e ->
                        val key = Uri.decode(e.arguments?.getString("key") ?: "")
                        val card = e.arguments?.getString("card")
                        val msg = e.arguments?.getString("msg")   // AII-105: a search result opens the chat at its message
                        val ref = repo.store.cachedChats()?.firstOrNull { it.key == key }
                        if (ref == null) LaunchedEffect(Unit) { nav.popBackStack() }
                        else ChatScreen(repo, ref, onBack = { nav.popBackStack() }, focus = card, openAt = msg,
                            onOpenCard = { id -> nav.navigate("chat/" + Uri.encode(key) + "?card=" + Uri.encode(id)) })
                    }
                    composable("new") {
                        // Leaving New message (sent, back button or gesture) drops the shared text.
                        DisposableEffect(Unit) { onDispose { shared = null } }
                        NewMessageScreen(repo, draft = shared ?: "", onBack = { nav.popBackStack() }, onSent = { to ->
                            nav.popBackStack(); nav.openChat(to.key)
                        })
                    }
                    composable("settings") {
                        SettingsScreen(repo, onBack = { nav.popBackStack() }, onSignedOut = {
                            nav.navigate("connect") { popUpTo(0) { inclusive = true } }
                        })
                    }
                } }
                // A new pairing link while running: back to Connect, filled in.
                LaunchedEffect(pairing) { if (pairing != null && nav.currentDestination?.route != "connect") nav.navigate("connect") }
                // Shared from another app: straight to New message (after Connect if not signed in yet).
                LaunchedEffect(shared) {
                    if (shared != null && repo.store.connected) nav.navigate("new") { launchSingleTop = true }
                }
                // New message from the app icon menu (AII-53).
                LaunchedEffect(openNew) {
                    if (!openNew) return@LaunchedEffect
                    if (repo.store.connected) nav.navigate("new") { launchSingleTop = true }
                    openNew = false
                }
                LaunchedEffect(openChat) {
                    val c = openChat ?: return@LaunchedEffect
                    if (repo.store.connected) nav.openChat(c)
                    openChat = null
                }
                // Covers everything until unlocked; asks again each time it locks.
                if (locked) {
                    LockedScreen(onUnlock = { unlock() })
                    LaunchedEffect(Unit) { unlock() }
                }
            }
        }
    }

    private fun NavHostController.openChat(key: String, msg: String? = null) =
        navigate("chat/" + Uri.encode(key) + (msg?.let { "?msg=" + Uri.encode(it) } ?: "")) { launchSingleTop = true }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        read(intent)
    }

    private fun read(intent: Intent?) {
        intent?.data?.toString()?.let { Mapper.parsePairing(it) }?.let { pairing = it }
        intent?.getStringExtra(CheckWorker.EXTRA_CHAT)?.let { openChat = it }
        if (intent?.action == Intent.ACTION_SEND)
            Share.text(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT))?.let { shared = it }
        if (intent?.action == Shortcuts.ACTION_NEW) openNew = true
    }

    private fun unlock() = Lock.ask(this) { unlocked = true; locked = false }

    override fun onStart() {
        super.onStart()
        App.inForeground = true
        val lockOn = (application as App).repo.store.lock
        // No chat preview in the recent apps list while the lock is on.
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!lockOn)
        if (Lock.shouldLock(lockOn, unlocked, leftAt, SystemClock.elapsedRealtime())) {
            unlocked = false; locked = true
        }
    }
    override fun onStop() { super.onStop(); App.inForeground = false; leftAt = SystemClock.elapsedRealtime() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("unlocked", unlocked)
        outState.putLong("leftAt", leftAt ?: SystemClock.elapsedRealtime())
    }
}
