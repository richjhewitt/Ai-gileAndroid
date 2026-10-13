package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.ChatsScreen
import com.smizflark.aigile.messenger.ui.ConnectScreen
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.Appearance
import com.smizflark.aigile.messenger.ui.Look
import com.smizflark.aigile.messenger.ui.TextSize
import com.smizflark.aigile.messenger.ui.NewMessageScreen
import com.smizflark.aigile.messenger.ui.EmptyState
import com.smizflark.aigile.messenger.ui.SettingsScreen
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-20: every screen, in the demo data, light and dark. Also proves none of them crashes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var repo: Repo
    private lateinit var chats: List<ChatRef>

    @Before fun setUp() {
        repo = Repo(ApplicationProvider.getApplicationContext<Application>())
        repo.startDemo()
        chats = runBlocking { repo.chats() }
    }

    private fun shot(name: String, dark: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers(if (dark) "+night" else "+notnight")
        rule.setContent { MessengerTheme { content() } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/${name}_${if (dark) "dark" else "light"}.png")
    }


    private fun lead() = chats.first { it.id == "ai-gile" }
    private fun prime(ref: ChatRef) { runBlocking { repo.conversation(ref) } }

    @Test fun connectLight() = shot("connect", false) { ConnectScreen(repo, null) {} }
    @Test fun connectDark() = shot("connect", true) { ConnectScreen(repo, null) {} }
    @Test fun chatsLight() = shot("chats", false) { ChatsScreen(repo, {}, {}, {}) }
    @Test fun chatsDark() = shot("chats", true) { ChatsScreen(repo, {}, {}, {}) }
    @Test fun chatLight() { prime(lead()); shot("chat", false) { ChatScreen(repo, lead(), onBack = {}) } }
    @Test fun chatDark() { prime(lead()); shot("chat", true) { ChatScreen(repo, lead(), onBack = {}) } }
    @Test fun chatHuge() {
        prime(lead()); Appearance.textSize = TextSize.Huge
        try { shot("chat_huge", false) { ChatScreen(repo, lead(), onBack = {}) } } finally { Appearance.textSize = TextSize.Normal }
    }
    @Test fun newLight() = shot("new_message", false) { NewMessageScreen(repo, onBack = {}, onSent = {}) }
    @Test fun newDark() = shot("new_message", true) { NewMessageScreen(repo, onBack = {}, onSent = {}) }
    @Test fun emptyChatsLight() = shot("empty_chats", false) { EmptyState("No chats yet", button = "New message") }
    @Test fun emptySearchDark() = shot("empty_search", true) { EmptyState("No results for “zebra”", button = "Clear search") }
    @Test fun settingsLight() = shot("settings", false) { SettingsScreen(repo, {}, {}) }
    @Test fun settingsDark() = shot("settings", true) { SettingsScreen(repo, {}, {}) }

    /** AII-69: the pure black (OLED) look, so every screen can be checked for grey panels. */
    private fun black(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        Appearance.look = Look.Black; Appearance.phoneColours = false
        try {
            rule.setContent { MessengerTheme { content() } }
            rule.waitForIdle()
            rule.onRoot().captureRoboImage("../docs/shots/${name}_black.png")
        } finally { Appearance.look = null; Appearance.phoneColours = true }
    }
    @Test fun chatsBlack() = black("chats") { ChatsScreen(repo, {}, {}, {}) }
    @Test fun chatBlack() { prime(lead()); black("chat") { ChatScreen(repo, lead(), onBack = {}) } }
    @Test fun newBlack() = black("new_message") { NewMessageScreen(repo, onBack = {}, onSent = {}) }
    @Test fun settingsBlack() = black("settings") { SettingsScreen(repo, {}, {}) }
}
