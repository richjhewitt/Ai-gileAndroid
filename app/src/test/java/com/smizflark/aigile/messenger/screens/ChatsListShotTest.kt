package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.ChatsScreen
import com.smizflark.aigile.messenger.ui.ConnectScreen
import com.smizflark.aigile.messenger.ui.SearchScreen
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.Appearance
import com.smizflark.aigile.messenger.ui.Look
import com.smizflark.aigile.messenger.ui.TextSize
import com.smizflark.aigile.messenger.ui.NewMessageScreen
import com.smizflark.aigile.messenger.ui.EmptyState
import com.smizflark.aigile.messenger.ui.LockedScreen
import com.smizflark.aigile.messenger.ui.SettingsScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
/** AII-20: the chats list, light and dark: rows, chips, menus and Undo bars. Shared setup in [ShotBase]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ChatsListShotTest : ShotBase() {
    @Test fun chatsLight() = shot("chats", false) { ChatsScreen(repo, {}, {}, {}) }
    @Test fun pausedLight() {
        repo.store.pausedUntil = Now.today().plusDays(1).atTime(8, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        shot("chats_paused", false) { ChatsScreen(repo, {}, {}, {}) }
        repo.store.pausedUntil = 0L
    }
    @Test fun newVersionLight() {   // AII-132
        repo.store.release = com.smizflark.aigile.messenger.data.Release(com.smizflark.aigile.messenger.BuildConfig.VERSION_CODE + 1, "https://example.com/a.apk")
        shot("chats_new_version", false) { ChatsScreen(repo, {}, {}, {}) }
        rule.onNodeWithTag("newVersion").assertExists()
    }
    @Test fun noBarWhenUpToDate() {
        repo.store.release = com.smizflark.aigile.messenger.data.Release(com.smizflark.aigile.messenger.BuildConfig.VERSION_CODE, "https://example.com/a.apk")
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        rule.onNodeWithTag("newVersion").assertDoesNotExist()
    }
    @Test fun draftLight() {
        repo.store.saveDraft(lead().key, "Sounds good, I'll check the\nscreenshot tonight")
        shot("chats_draft", false) { ChatsScreen(repo, {}, {}, {}) }
        repo.store.saveDraft(lead().key, "")
    }
    /** AII-148: a chat with a Send later message says when it goes. */
    @Test fun sendsAtLight() {
        repo.queue(lead(), "Morning team", null, sendAt = java.time.Instant.parse("2026-10-14T07:00:00Z"))
        shot("chats_sends_at", false) { ChatsScreen(repo, {}, {}, {}) }
        rule.onNodeWithText("⏰ Sends at 08:00").assertIsDisplayed()
    }
    /** AII-152: with a Send later message waiting, the Scheduled chip keeps just that chat. */
    @Test fun scheduledChip() {
        RuntimeEnvironment.setQualifiers("+notnight")
        val l = lead()
        repo.queue(l, "Morning team", null, sendAt = java.time.Instant.parse("2026-10-14T07:00:00Z"))
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("filter:scheduled").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("filter:scheduled").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("chat:${l.key}").assertIsDisplayed()
        org.junit.Assert.assertTrue(chats.filter { it.key != l.key }.all { c -> rule.onAllNodesWithTag("chat:${c.key}").fetchSemanticsNodes().isEmpty() })
        rule.onRoot().captureRoboImage("../docs/shots/chats_scheduled_light.png")
    }
    /** AII-103: hold a chat: Mute sits in the menu, and tapping it again unmutes. */
    @Test fun muteFromMenuLight() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        rule.onNodeWithTag("chat:${c.key}").performTouchInput { longClick() }
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chats_menu_light.png")   // the menu is its own window: shoot the whole screen
        rule.onNodeWithTag("menu-mute").performClick()
        // AII-157: Mute asks how long.
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chats_mute_for_light.png")
        rule.onNodeWithTag("mute-for:HOUR").performClick()
        org.junit.Assert.assertTrue(c.key in repo.store.muted())
        // AII-165: the row shows when the mute ends.
        rule.waitForIdle()
        rule.onNodeWithTag("row-mute-ends", useUnmergedTree = true).assertExists()
        rule.onRoot().captureRoboImage("../docs/shots/chats_mute_ends_light.png")
        rule.onNodeWithTag("chat:${c.key}").performTouchInput { longClick() }
        rule.onNodeWithTag("menu-mute").performClick()
        org.junit.Assert.assertFalse(c.key in repo.store.muted())
    }
    /** AII-119: hold a read chat: Mark as unread brings its badge back. */
    @Test fun markUnreadLight() {
        val c = lead(); prime(c); runBlocking { repo.markRead(c) }
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        rule.waitForIdle()
        org.junit.Assert.assertTrue(rule.onAllNodesWithTag("unread:${c.key}", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("chat:${c.key}").performTouchInput { longClick() }
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chats_unread_menu_light.png")
        rule.onNodeWithTag("menu-unread").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("unread:${c.key}", useUnmergedTree = true).assertExists()
    }
    /** AII-131: hold a chat, Close chat: it waits under 'Show 1 closed' at the bottom; Reopen brings it back. */
    @Test fun closeChatLight() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        rule.onNodeWithTag("chat:${c.key}").performTouchInput { longClick() }
        rule.onNodeWithTag("menu-close").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("chat:${c.key}").assertDoesNotExist()
        rule.onNodeWithTag("show-closed").performClick()
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chats_closed_light.png")
        rule.onNodeWithTag("chat:${c.key}").performTouchInput { longClick() }
        rule.onNodeWithTag("menu-close").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertFalse(c.key in repo.store.closed())
    }
    /** AII-154: Close chat shows a bar with Undo, which puts the chat straight back. */
    @Test fun closeChatUndo() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("chat:${c.key}").performTouchInput { longClick() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("menu-close").performClick()
        rule.mainClock.advanceTimeBy(500)
        org.junit.Assert.assertTrue(c.key in repo.store.closed())
        rule.onNodeWithText("Chat closed").assertExists()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chats_closed_undo_light.png")
        rule.onNodeWithText("Undo").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        org.junit.Assert.assertFalse(c.key in repo.store.closed())
        rule.onNodeWithTag("chat:${c.key}").assertExists()
    }
    /** AII-155: a search shows under Recent searches next time; tap it to search again, Clear empties the list. */
    @Test fun recentSearchesLight() {
        RuntimeEnvironment.setQualifiers("+notnight")
        repo.store.setRecentSearches(listOf("export", "playlist"))
        rule.setContent { MessengerTheme { SearchScreen(repo, { _, _ -> }, {}) } }
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/search_recent_light.png")
        rule.onNodeWithTag("recent:playlist").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertEquals(listOf("playlist", "export"), repo.store.recentSearches())
        rule.onNodeWithTag("recent").assertDoesNotExist()
        rule.onNodeWithContentDescription("Clear").performClick()
        rule.onNodeWithTag("search").performTextInput("tunio")
        rule.onNodeWithTag("search").performImeAction()
        rule.waitForIdle()
        org.junit.Assert.assertEquals(listOf("tunio", "playlist", "export"), repo.store.recentSearches())
        rule.onNodeWithContentDescription("Clear").performClick()
        rule.onNodeWithTag("recent-clear").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertEquals(emptyList<String>(), repo.store.recentSearches())
    }
    /** AII-158: Mark all as read shows a bar with Undo, which brings the badges back. */
    @Test fun readAllUndo() {
        val c = lead(); prime(c)
        val before = repo.store.seen()[c.key]
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("readAll").performClick()
        rule.mainClock.advanceTimeBy(500)
        org.junit.Assert.assertNotEquals(before, repo.store.seen()[c.key])
        rule.onNodeWithText("All read").assertExists()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chats_read_undo_light.png")
        rule.onNodeWithText("Undo").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        org.junit.Assert.assertEquals(before, repo.store.seen()[c.key])
    }
    /** AII-161: after Mute, a bar with Undo unmutes the chat. */
    @Test fun muteUndo() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("chat:${c.key}").performTouchInput { longClick() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("menu-mute").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("mute-for:HOUR").performClick()
        rule.mainClock.advanceTimeBy(500)
        org.junit.Assert.assertTrue(c.key in repo.store.muted())
        rule.onNodeWithText("Undo").assertExists()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chats_mute_undo_light.png")
        rule.onNodeWithText("Undo").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        org.junit.Assert.assertFalse(c.key in repo.store.muted())
    }
    /** AII-125: the Unread chip keeps only chats with a badge. */
    @Test fun unreadChipLight() {
        val c = lead(); prime(c); runBlocking { repo.markRead(c) }
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        rule.onNodeWithTag("filter:unread").performClick()
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chats_unread_chip_light.png")
        org.junit.Assert.assertTrue(rule.onAllNodesWithTag("chat:${c.key}").fetchSemanticsNodes().isEmpty())
    }
    @Test fun chatsDark() = shot("chats", true) { ChatsScreen(repo, {}, {}, {}) }
}
