package com.smizflark.aigile.messenger

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.KeyEvent
import android.view.Menu
import androidx.compose.runtime.SideEffect
import com.smizflark.aigile.messenger.data.Closed
import com.smizflark.aigile.messenger.data.KeyAction
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.keyAction
import com.smizflark.aigile.messenger.data.mutedLast
import com.smizflark.aigile.messenger.data.neighbourChat
import com.smizflark.aigile.messenger.data.pinFirst
import com.smizflark.aigile.messenger.data.sortChats
import com.smizflark.aigile.messenger.ui.Appearance
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Share
import com.smizflark.aigile.messenger.data.ShareTargets
import com.smizflark.aigile.messenger.ui.MAX_PICS
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.ChatsPanes
import com.smizflark.aigile.messenger.ui.ChatsScreen
import com.smizflark.aigile.messenger.ui.isWide
import com.smizflark.aigile.messenger.ui.ConnectScreen
import com.smizflark.aigile.messenger.ui.Lock
import com.smizflark.aigile.messenger.ui.LockedScreen
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.NewMessageScreen
import com.smizflark.aigile.messenger.ui.PictureHost
import com.smizflark.aigile.messenger.ui.SearchScreen
import com.smizflark.aigile.messenger.ui.AboutScreen
import com.smizflark.aigile.messenger.ui.SettingsScreen
import com.smizflark.aigile.messenger.ui.RemindersScreen
import com.smizflark.aigile.messenger.ui.StarredScreen
import com.smizflark.aigile.messenger.ui.WaitingScreen
import com.smizflark.aigile.messenger.ui.Shortcuts
import com.smizflark.aigile.messenger.work.CheckWorker

/** The Connect screen's route; out=true after Sign out puts Scan QR code first (AII-289). */
private const val CONNECT = "connect?out={out}"

class MainActivity : FragmentActivity() {
    /** A pairing link (aigile://pair?...) or a chat to open from an alert. */
    private var pairing by mutableStateOf<Pair<String, String>?>(null)
    private var openChat by mutableStateOf<String?>(null)
    /** The message to land on when the chat comes from a reminder alert (AII-201). */
    private var openMsg by mutableStateOf<String?>(null)
    /** Text shared in from another app, waiting to be sent from New message. */
    private var shared by mutableStateOf<String?>(null)
    /** Pictures shared in from another app (AII-338), kept in the outbox folder, sent with the shared text. */
    private var sharedPics by mutableStateOf<List<String>>(emptyList())
    /** The chat tapped in the share sheet (AII-398, EXTRA_SHORTCUT_ID), already picked in New message. */
    private var sharedTo by mutableStateOf<String?>(null)
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
                var selected by rememberSaveable { mutableStateOf<String?>(null) }   // AII-350: the chat open in the right pane
                val wideNow = isWide(LocalConfiguration.current.screenWidthDp.toFloat(), LocalConfiguration.current.screenHeightDp.toFloat())
                SideEffect {   // AII-408: hardware keyboard shortcuts, see dispatchKeyEvent
                    keyAct = { act ->
                        val route = nav.currentDestination?.route
                        if (repo.store.connected && !locked) when (act) {
                            KeyAction.BACK -> onBackPressedDispatcher.onBackPressed()
                            KeyAction.SEARCH -> if (route != "search" && route != CONNECT) nav.navigate("search") { launchSingleTop = true }
                            KeyAction.NEW_MESSAGE -> if (route != "new" && route != CONNECT) nav.navigate("new") { launchSingleTop = true }
                            KeyAction.NEXT_CHAT, KeyAction.PREVIOUS_CHAT -> {
                                val inChat = route?.startsWith("chat/") == true
                                if (inChat || route == "chats") {
                                    val cur = if (inChat) nav.currentBackStackEntry?.arguments?.getString("key")?.let { Uri.decode(it) } else if (wideNow) selected else null
                                    val next = neighbourChat(chatOrder(repo), cur, if (act == KeyAction.NEXT_CHAT) 1 else -1)
                                    if (next != null) when {
                                        inChat -> { nav.popBackStack(); nav.openChat(next) }
                                        wideNow -> selected = next
                                        else -> nav.openChat(next)
                                    }
                                }
                            }
                        }
                    }
                }
                val start = if (repo.store.connected) "chats" else CONNECT
                PictureHost { NavHost(nav, startDestination = start) {
                    composable(CONNECT, arguments = listOf(navArgument("out") { defaultValue = false; type = NavType.BoolType })) { e ->
                        ConnectScreen(repo, pairing, scanFirst = e.arguments?.getBoolean("out") == true) {   // AII-289
                            pairing = null
                            if (repo.store.alerts && !repo.store.demo) CheckWorker.schedule(this@MainActivity)
                            nav.navigate("chats") { popUpTo(CONNECT) { inclusive = true } }
                            if (shared != null) nav.navigate("new")
                        }
                    }
                    composable("chats") {
                        // AII-350: tablets and unfolded foldables show the open chat beside the list; a phone (or a fold closed again) opens it full screen as before.
                        ChatsPanes(repo, selected, onSelect = { selected = it }, onWide = { wide ->
                            if (!wide) selected?.let { nav.openChat(it); selected = null }
                        }, list = { open ->
                            ChatsScreen(repo, onOpen = open, onNew = { nav.navigate("new") },
                                onSettings = { nav.navigate("settings") }, onSearch = { nav.navigate("search") }, onStarred = { nav.navigate("starred") },
                                onWaiting = { nav.navigate("waiting") }, onWhatsNew = { nav.navigate("about") })
                        }, chat = { ref ->
                            ChatScreen(repo, ref, onBack = { selected = null },
                                onEditReplies = { nav.navigate("settings?replies=true") },
                                onOpenChat = { next -> selected = next.key },
                                onOpenCard = { id -> nav.navigate("chat/" + Uri.encode(ref.key) + "?card=" + Uri.encode(id)) })
                        })
                    }
                    composable("waiting") {   // AII-166: every Send later message; tap one, its chat opens at it
                        WaitingScreen(repo, onOpen = { ref, msg -> nav.openChat(ref.key, msg) }, onBack = { nav.popBackStack() })
                    }
                    composable("reminders") {   // AII-209: see and cancel reminders
                        RemindersScreen(repo, onOpen = { chat, msg -> nav.openChat(chat, msg) }, onBack = { nav.popBackStack() })
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
                        val wide = isWide(LocalConfiguration.current.screenWidthDp.toFloat(), LocalConfiguration.current.screenHeightDp.toFloat())
                        if (ref == null) LaunchedEffect(Unit) { nav.popBackStack() }
                        else if (wide && card == null && msg == null && nav.previousBackStackEntry?.destination?.route == "chats") {   // AII-350: turned or unfolded while reading: back to the two panes
                            LaunchedEffect(key) { selected = key; nav.popBackStack() }
                        }
                        else ChatScreen(repo, ref, onBack = { nav.popBackStack() }, focus = card, openAt = msg,
                            onEditReplies = { nav.navigate("settings?replies=true") },   // AII-255
                            onOpenChat = { next -> nav.popBackStack(); nav.openChat(next.key) },   // AII-204: as if tapped in the chats list
                            onOpenCard = { id -> nav.navigate("chat/" + Uri.encode(key) + "?card=" + Uri.encode(id)) })
                    }
                    composable("new") {
                        // Leaving New message (sent, back button or gesture) drops the shared text.
                        DisposableEffect(Unit) { onDispose { shared = null; sharedPics = emptyList(); sharedTo = null } }
                        NewMessageScreen(repo, draft = shared ?: "", pics = sharedPics, preselect = sharedTo, onBack = { nav.popBackStack() }, onSent = { to ->
                            nav.popBackStack(); nav.openChat(to.key)
                        })
                    }
                    composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }   // AII-220
                    composable("settings?replies={replies}", arguments = listOf(navArgument("replies") { defaultValue = false; type = NavType.BoolType })) { e ->
                        SettingsScreen(repo, showReplies = e.arguments?.getBoolean("replies") == true, onBack = { nav.popBackStack() }, onReminders = { nav.navigate("reminders") }, onAbout = { nav.navigate("about") }, onSignedOut = { wasDemo ->   // AII-289: a real Sign out leads with Scan QR code; Leave the demo goes back to the first screen (AII-278)
                            nav.navigate(if (wasDemo) "connect" else "connect?out=true") { popUpTo(0) { inclusive = true } }
                        })
                    }
                } }
                // A new pairing link while running: back to Connect, filled in.
                LaunchedEffect(pairing) { if (pairing != null && nav.currentDestination?.route != CONNECT) nav.navigate("connect") }
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
                    if (repo.store.connected) nav.openChat(c, openMsg)
                    openChat = null; openMsg = null
                }
                // Covers everything until unlocked; asks again each time it locks.
                if (locked) {
                    LockedScreen(onUnlock = { unlock() })
                    LaunchedEffect(Unit) { unlock() }
                }
            }
        }
    }

    private var keyAct: ((KeyAction) -> Unit)? = null

    /** The chats in the order the chats list shows them, without closed ones (AII-408). */
    private fun chatOrder(repo: com.smizflark.aigile.messenger.data.Repo): List<String> {
        val all = repo.store.cachedChats().orEmpty()
        val cl = repo.store.closed()
        val seen = repo.store.seen()
        val shown = all.filter { !Closed.hidden(it, cl[it.key]) }
        val sorted = sortChats(shown, Appearance.sort) { News.fresh(it, seen[it.key]) }
        val pinned = repo.store.pinned
        return (if (Appearance.mutedLast) mutedLast(sorted, pinned, repo.store.muted()) else pinFirst(sorted, pinned)).map { it.key }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
            val act = keyAction(event.keyCode, event.isCtrlPressed, event.isAltPressed, event.isShiftPressed, event.isMetaPressed)
            if (act != null && keyAct != null) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) keyAct?.invoke(act)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /** Keyboard Shortcuts Helper (hold Meta, or Ctrl+Alt+/): lists the shortcuts above (AII-408). */
    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>, menu: Menu?, deviceId: Int) {
        val ctrl = KeyEvent.META_CTRL_ON
        val alt = KeyEvent.META_ALT_ON
        data.add(KeyboardShortcutGroup(getString(R.string.app_name), listOf(
            KeyboardShortcutInfo(getString(R.string.key_search), KeyEvent.KEYCODE_F, ctrl),
            KeyboardShortcutInfo(getString(R.string.key_new), KeyEvent.KEYCODE_N, ctrl),
            KeyboardShortcutInfo(getString(R.string.key_back), KeyEvent.KEYCODE_ESCAPE, 0),
            KeyboardShortcutInfo(getString(R.string.key_next_chat), KeyEvent.KEYCODE_DPAD_DOWN, alt),
            KeyboardShortcutInfo(getString(R.string.key_prev_chat), KeyEvent.KEYCODE_DPAD_UP, alt),
        )))
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
    }

    private fun NavHostController.openChat(key: String, msg: String? = null) =
        navigate("chat/" + Uri.encode(key) + (msg?.let { "?msg=" + Uri.encode(it) } ?: "")) { launchSingleTop = true }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        read(intent)
    }

    private fun read(intent: Intent?) {
        intent?.data?.toString()?.let { Mapper.parsePairing(it) }?.let { pairing = it }
        intent?.getStringExtra(CheckWorker.EXTRA_CHAT)?.let { openChat = it; openMsg = intent.getStringExtra(CheckWorker.EXTRA_MSG) }
        if (intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE)
            sharedTo = ShareTargets.chatKey(intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID))
        if (intent?.action == Intent.ACTION_SEND)
            Share.text(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT))?.let { shared = it }
        // AII-338: pictures shared in open New message at once; the copies follow when they are made.
        val uris = sharedImages(intent)
        if (uris.isNotEmpty()) {
            shared = shared ?: ""; sharedPics = emptyList()
            val repo = (application as App).repo
            lifecycleScope.launch {
                val kept = withContext(Dispatchers.IO) { uris.take(MAX_PICS).mapNotNull { repo.keepPicture(it) } }
                if (shared != null) sharedPics = kept else kept.forEach { java.io.File(it).delete() }
            }
        }
        if (intent?.action == Shortcuts.ACTION_NEW) openNew = true
    }

    /** The pictures in a Share or Share many from another app (AII-338). */
    @Suppress("DEPRECATION")
    private fun sharedImages(intent: Intent?): List<Uri> {
        if (intent?.type?.startsWith("image/") != true) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            else -> emptyList()
        }
    }

    private fun unlock() = Lock.ask(this) { unlocked = true; locked = false }

    override fun onStart() {
        super.onStart()
        App.inForeground = true
        com.smizflark.aigile.messenger.data.Secure.apply(window, (application as App).repo.store.blockShots)
        val lockOn = (application as App).repo.store.lock
        // No chat preview in the recent apps list while the lock is on.
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!lockOn)
        if (Lock.shouldLock(lockOn, unlocked, leftAt, SystemClock.elapsedRealtime(), (application as App).repo.store.lockAfter)) {
            unlocked = false; locked = true
        }
    }
    override fun onStop() {
        super.onStop(); App.inForeground = false
        if (!isChangingConfigurations) leftAt = SystemClock.elapsedRealtime()   // turning the phone is not leaving
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("unlocked", unlocked)
        leftAt?.let { outState.putLong("leftAt", it) }
    }
}
