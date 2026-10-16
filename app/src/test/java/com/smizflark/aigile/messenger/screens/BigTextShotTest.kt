package com.smizflark.aigile.messenger.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.ChatsScreen
import com.smizflark.aigile.messenger.ui.NewMessageScreen
import com.smizflark.aigile.messenger.ui.SearchScreen
import com.smizflark.aigile.messenger.ui.SettingsScreen
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-410: the main screens at Android's largest font size (2x), light. Files docs/shots/big_*_light.png. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class BigTextShotTest : ShotBase() {
    @Before fun bigFont() { RuntimeEnvironment.setFontScale(2.0f) }
    @After fun normalFont() { RuntimeEnvironment.setFontScale(1.0f) }

    private fun davy() = chats.first { it.kind == Kind.FRIEND }
    private fun big(name: String) = "../docs/shots/big_${name}_light.png"

    @Test fun chatsList() = shot("big_chats", false) { ChatsScreen(repo, {}, {}, {}) }
    @Test fun chat() { prime(lead()); shot("big_chat", false) { ChatScreen(repo, lead(), onBack = {}) } }
    @Test fun friendChat() { prime(davy()); shot("big_friend", false) { ChatScreen(repo, davy(), onBack = {}) } }
    @Test fun settings() = shot("big_settings", false) { SettingsScreen(repo, {}, {}) }
    @Test fun newMessage() = shot("big_new_message", false) { NewMessageScreen(repo, onBack = {}, onSent = {}) }

    @Test fun details() {
        prime(lead())
        show(false) { ChatScreen(repo, lead(), onBack = {}) }
        rule.onNodeWithTag("chat-title").performClick()
        rule.onNodeWithTag("details-pin").fetchSemanticsNode()
        rule.waitForIdle()
        rule.onAllNodes(androidx.compose.ui.test.isRoot()).onLast().captureRoboImage(big("details"))   // the sheet is its own window
    }

    @Test fun search() {
        chats.forEach { prime(it) }
        val zed = com.smizflark.aigile.messenger.data.ChatRef(Kind.FRIEND, "zed", "Zed", "", "🙂")
        repo.store.cacheChats(repo.store.cachedChats().orEmpty() + zed)
        repo.store.cacheConversation(com.smizflark.aigile.messenger.data.Conversation(zed, listOf(
            com.smizflark.aigile.messenger.data.Bubble("t1", "Zed", "Read [the Guide](https://dnbloco.com/guide) first, **Guide** matters", "2026-10-12T09:00:00Z", mine = false))))
        show(false) { SearchScreen(repo, { _, _ -> }, {}) }
        rule.onNodeWithTag("search").performTextInput("guide")
        rule.waitForIdle()
        rule.onRoot().captureRoboImage(big("search"))
    }

    @Test fun holdMenuAndInfo() {
        prime(lead())
        show(false) { ChatScreen(repo, lead(), onBack = {}) }
        rule.onAllNodesWithTag("theirs").onLast().performTouchInput { longClick() }
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage(big("hold_menu"))
        rule.onNodeWithTag("info").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("info-sent").assertIsDisplayed()
        com.github.takahirom.roborazzi.captureScreenRoboImage(big("message_info"))
    }
}
