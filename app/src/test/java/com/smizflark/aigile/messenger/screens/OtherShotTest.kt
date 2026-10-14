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
/** AII-20: Connect, Starred, New message, Settings, lock, and the black and Dutch looks. Shared setup in [ShotBase]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class OtherShotTest : ShotBase() {
    @Test fun connectLight() = shot("connect", false) { ConnectScreen(repo, null) {} }
    @Test fun connectDark() = shot("connect", true) { ConnectScreen(repo, null) {} }
    /** AII-138: hold a message, Star: a ★ by its time, and it shows under Starred in the details; tapping it closes the sheet. */
    @Test fun starredLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onAllNodesWithTag("theirs").onLast().performTouchInput { longClick() }
        rule.onNodeWithTag("star").performClick()
        rule.onAllNodes(androidx.compose.ui.test.hasText("★", substring = true), useUnmergedTree = true).onFirst().assertExists()
        rule.onNodeWithTag("chat-title").performClick()
        rule.onNodeWithTag("details-star").assertExists()
        rule.onNodeWithTag("details-share").assertExists()   // AII-139
        rule.waitForIdle()
        rule.onAllNodes(androidx.compose.ui.test.isRoot()).onLast().captureRoboImage("../docs/shots/chat_starred_light.png")   // the sheet is its own window
        rule.onNodeWithTag("details-star").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("chat-details").fetchSemanticsNodes().isEmpty() }
    }
    /** AII-140: stars in two chats; the Starred button on the chats list opens them all, newest first, and a tap opens that chat at it. */
    @Test fun starredEverywhereLight() {
        val two = chats.filter { it.kind == com.smizflark.aigile.messenger.data.Kind.LEAD }.take(2)
        two.forEach { prime(it) }
        repo.store.setStars(two.map { c -> com.smizflark.aigile.messenger.data.Stars.of(c.key, repo.store.cachedConversation(c.key)!!.bubbles.last { !it.mine }) })
        var opened: Pair<ChatRef, String>? = null
        var starred by androidx.compose.runtime.mutableStateOf(false)
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme {
            if (starred) com.smizflark.aigile.messenger.ui.StarredScreen(repo, onOpen = { r, k -> opened = r to k }, onBack = {})
            else ChatsScreen(repo, onOpen = {}, onNew = {}, onSettings = {}, onStarred = { starred = true })
        } }
        rule.onNodeWithTag("open-starred").performClick()
        rule.onAllNodesWithTag("starred-row").assertCountEquals(2)
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/starred_light.png")
        rule.onAllNodesWithTag("starred-row").onFirst().performClick()
        org.junit.Assert.assertEquals(repo.store.stars().maxBy { it.at }.key, opened?.second)
        // AII-143: hold one, Remove star: it leaves the list.
        rule.onAllNodesWithTag("starred-row").onFirst().performTouchInput { longClick() }
        rule.onNodeWithTag("starred-unstar").performClick()
        rule.onAllNodesWithTag("starred-row").assertCountEquals(1)
        org.junit.Assert.assertEquals(1, repo.store.stars().size)
        // AII-156: Undo puts it back.
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/starred_undo_light.png")
        rule.onNodeWithText("Undo").performClick()
        rule.onAllNodesWithTag("starred-row").assertCountEquals(2)
        org.junit.Assert.assertEquals(2, repo.store.stars().size)
        // AII-164: the search box keeps only the stars with those words in them.
        val q = Regex("[A-Za-z]{5,}").find(repo.store.stars().first().text)!!.value
        rule.onNodeWithTag("starred-search").performTextInput(q)
        rule.onAllNodesWithTag("starred-row").assertCountEquals(repo.store.stars().count { it.text.contains(q, ignoreCase = true) })
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/starred_search_light.png")
        rule.onNodeWithTag("starred-search").performTextReplacement("zzzq")
        rule.onAllNodesWithTag("starred-row").assertCountEquals(0)
        rule.onNodeWithText("No results for “zzzq”").assertExists()
    }
    @Test fun newLight() = shot("new_message", false) { NewMessageScreen(repo, onBack = {}, onSent = {}) }
    @Test fun newDark() = shot("new_message", true) { NewMessageScreen(repo, onBack = {}, onSent = {}) }
    @Test fun emptyChatsLight() = shot("empty_chats", false) { EmptyState("No chats yet", button = "New message") }
    @Test fun emptySearchDark() = shot("empty_search", true) { EmptyState("No results for “zebra”", button = "Clear search") }
    /** AII-192: Search with the Links chip on and no query. */
    @Test fun searchLinksLight() {
        chats.forEach { prime(it) }
        // A chat the demo doesn't know, so the screen's own fetch leaves the seeded messages alone.
        val zed = com.smizflark.aigile.messenger.data.ChatRef(com.smizflark.aigile.messenger.data.Kind.FRIEND, "zed", "Zed", "", "🙂")
        repo.store.cacheChats(repo.store.cachedChats().orEmpty() + zed)
        fun m(k: String, t: String, at: String) = com.smizflark.aigile.messenger.data.Bubble(k, "Zed", t, at, mine = false)
        repo.store.cacheConversation(com.smizflark.aigile.messenger.data.Conversation(zed, listOf(
            m("l1", "Read [the guide](https://dnbloco.com/guide) first", "2026-10-12T09:00:00Z"),
            m("l2", "Then open dnbloco.com and check it loads.", "2026-10-13T09:00:00Z"))))
        show(false) { SearchScreen(repo, { _, _ -> }, {}) }
        rule.onNodeWithTag("search-links").performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/search_links_light.png")
    }
    /** AII-195: typed word in bold, link shows its name. */
    @Test fun searchTidyLight() {
        chats.forEach { prime(it) }
        val zed = com.smizflark.aigile.messenger.data.ChatRef(com.smizflark.aigile.messenger.data.Kind.FRIEND, "zed", "Zed", "", "🙂")
        repo.store.cacheChats(repo.store.cachedChats().orEmpty() + zed)
        repo.store.cacheConversation(com.smizflark.aigile.messenger.data.Conversation(zed, listOf(
            com.smizflark.aigile.messenger.data.Bubble("t1", "Zed", "Read [the Guide](https://dnbloco.com/guide) first, **Guide** matters", "2026-10-12T09:00:00Z", mine = false))))
        show(false) { SearchScreen(repo, { _, _ -> }, {}) }
        rule.onNodeWithTag("search").performTextInput("guide")
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/search_tidy_light.png")
    }
    @Test fun aboutLight() = shot("about", false) { com.smizflark.aigile.messenger.ui.AboutScreen({}, build = 1) }
    @Test fun settingsLight()=shot("settings", false) { SettingsScreen(repo, {}, {}) }
    @Test fun settingsDark() = shot("settings", true) { SettingsScreen(repo, {}, {}) }
    /** AII-175: Settings has the Sent alert switch for Send later, on by default; turning it off is kept. */
    @Test fun settingsSentAlertsLight() {
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { SettingsScreen(repo, {}, {}) } }
        rule.onNodeWithTag("sentAlerts").performScrollTo().assertIsDisplayed()
        rule.onRoot().captureRoboImage("../docs/shots/settings_sent_alerts_light.png")
        org.junit.Assert.assertTrue(repo.store.sentAlerts)
        rule.onNodeWithTag("sentAlerts").onChildren().filterToOne(androidx.compose.ui.test.isToggleable()).performClick()
        org.junit.Assert.assertFalse(repo.store.sentAlerts)
    }

    /** AII-63: the lock switch in Settings, and the screen that covers the app until unlocked. */
    @Test fun lockedDark() = shot("locked", true) { LockedScreen {} }
    @Test fun settingsLockLight() {
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { SettingsScreen(repo, {}, {}) } }
        rule.onNodeWithText("New message alerts").performScrollTo()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/settings_lock_light.png")
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
