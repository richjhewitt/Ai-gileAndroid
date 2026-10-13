package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
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
import com.smizflark.aigile.messenger.ui.LockedScreen
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
    @Test fun pausedLight() {
        repo.store.pausedUntil = java.time.LocalDate.now().plusDays(1).atTime(8, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        shot("chats_paused", false) { ChatsScreen(repo, {}, {}, {}) }
        repo.store.pausedUntil = 0L
    }
    @Test fun chatsDark() = shot("chats", true) { ChatsScreen(repo, {}, {}, {}) }
    @Test fun chatLight() { prime(lead()); shot("chat", false) { ChatScreen(repo, lead(), onBack = {}) } }
    @Test fun chatDark() { prime(lead()); shot("chat", true) { ChatScreen(repo, lead(), onBack = {}) } }
    @Test fun chatHuge() {
        prime(lead()); Appearance.textSize = TextSize.Huge
        try { shot("chat_huge", false) { ChatScreen(repo, lead(), onBack = {}) } } finally { Appearance.textSize = TextSize.Normal }
    }
    /** AII-78: the find bar open on the lead chat, with a query that matches. */
    @Test fun findLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onNodeWithContentDescription("Find in this chat").performClick()
        rule.onNodeWithTag("find-field").performTextInput("phone")
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/chat_find_light.png")
    }
    /** AII-80: tap the chat name; the details sheet opens (dark, as the phone shows it at night). */
    @Test fun chatDetailsDark() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+night")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onNodeWithTag("chat-title").performClick()
        rule.onNodeWithTag("details-pin").fetchSemanticsNode()
        rule.waitForIdle()
        rule.onAllNodes(androidx.compose.ui.test.isRoot()).onLast().captureRoboImage("../docs/shots/chat_details_dark.png")   // the sheet is its own window
    }
    @Test fun newLight() = shot("new_message", false) { NewMessageScreen(repo, onBack = {}, onSent = {}) }
    @Test fun newDark() = shot("new_message", true) { NewMessageScreen(repo, onBack = {}, onSent = {}) }
    @Test fun emptyChatsLight() = shot("empty_chats", false) { EmptyState("No chats yet", button = "New message") }
    @Test fun emptySearchDark() = shot("empty_search", true) { EmptyState("No results for “zebra”", button = "Clear search") }
    @Test fun settingsLight() = shot("settings", false) { SettingsScreen(repo, {}, {}) }
    @Test fun settingsDark() = shot("settings", true) { SettingsScreen(repo, {}, {}) }

    /** AII-63: the lock switch in Settings, and the screen that covers the app until unlocked. */
    @Test fun lockedDark() = shot("locked", true) { LockedScreen {} }
    @Test fun settingsLockLight() {
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { SettingsScreen(repo, {}, {}) } }
        rule.onNodeWithText("New message alerts").performScrollTo()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/settings_lock_light.png")
    }

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

    /** AII-70: the phone set to Dutch. */
    @Test fun chatsDutch() {
        RuntimeEnvironment.setQualifiers("+nl-rNL")
        try { shot("chats_nl", false) { ChatsScreen(repo, {}, {}, {}) } } finally { RuntimeEnvironment.setQualifiers("+en-rUS") }
    }
    @Test fun settingsDutch() {
        RuntimeEnvironment.setQualifiers("+nl-rNL")
        try { shot("settings_nl", false) { SettingsScreen(repo, {}, {}) } } finally { RuntimeEnvironment.setQualifiers("+en-rUS") }
    }
}
