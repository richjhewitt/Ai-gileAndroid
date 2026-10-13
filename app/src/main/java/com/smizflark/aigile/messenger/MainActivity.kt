package com.smizflark.aigile.messenger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.NewMessageScreen
import com.smizflark.aigile.messenger.ui.SettingsScreen
import com.smizflark.aigile.messenger.work.CheckWorker

class MainActivity : ComponentActivity() {
    /** A pairing link (aigile://pair?...) or a chat to open from an alert. */
    private var pairing by mutableStateOf<Pair<String, String>?>(null)
    private var openChat by mutableStateOf<String?>(null)
    /** Text shared in from another app, waiting to be sent from New message. */
    private var shared by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        read(intent)
        val repo = (application as App).repo
        setContent {
            MessengerTheme(repo.store) {
                val nav = rememberNavController()
                val start = if (repo.store.connected) "chats" else "connect"
                NavHost(nav, startDestination = start) {
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
                            onSettings = { nav.navigate("settings") })
                    }
                    // A chat, or one card of it (?card=AIG-12, AII-28).
                    composable("chat/{key}?card={card}", arguments = listOf(navArgument("card") { type = NavType.StringType; nullable = true })) { e ->
                        val key = Uri.decode(e.arguments?.getString("key") ?: "")
                        val card = e.arguments?.getString("card")
                        val ref = repo.store.cachedChats()?.firstOrNull { it.key == key }
                        if (ref == null) LaunchedEffect(Unit) { nav.popBackStack() }
                        else ChatScreen(repo, ref, onBack = { nav.popBackStack() }, focus = card,
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
                }
                // A new pairing link while running: back to Connect, filled in.
                LaunchedEffect(pairing) { if (pairing != null && nav.currentDestination?.route != "connect") nav.navigate("connect") }
                // Shared from another app: straight to New message (after Connect if not signed in yet).
                LaunchedEffect(shared) {
                    if (shared != null && repo.store.connected) nav.navigate("new") { launchSingleTop = true }
                }
                LaunchedEffect(openChat) {
                    val c = openChat ?: return@LaunchedEffect
                    if (repo.store.connected) nav.openChat(c)
                    openChat = null
                }
            }
        }
    }

    private fun NavHostController.openChat(key: String) = navigate("chat/" + Uri.encode(key)) { launchSingleTop = true }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        read(intent)
    }

    private fun read(intent: Intent?) {
        intent?.data?.toString()?.let { Mapper.parsePairing(it) }?.let { pairing = it }
        intent?.getStringExtra(CheckWorker.EXTRA_CHAT)?.let { openChat = it }
        if (intent?.action == Intent.ACTION_SEND)
            Share.text(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT))?.let { shared = it }
    }

    override fun onStart() { super.onStart(); App.inForeground = true }
    override fun onStop() { super.onStop(); App.inForeground = false }
}
