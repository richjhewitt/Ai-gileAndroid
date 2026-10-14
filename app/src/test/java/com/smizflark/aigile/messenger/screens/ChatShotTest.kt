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
/** AII-20: one chat open: reading, replying, find, Send later, chat details. Shared setup in [ShotBase]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ChatShotTest : ShotBase() {
    @Test fun chatLight() { prime(lead()); shot("chat", false) { ChatScreen(repo, lead(), onBack = {}) } }
    @Test fun chatDark() { prime(lead()); shot("chat", true) { ChatScreen(repo, lead(), onBack = {}) } }
    @Test fun codeBoxDark() { val t = chats.first { it.id == "tunio" }; prime(t); shot("chat_code", true) { ChatScreen(repo, t, onBack = {}) } }   // AII-115
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
    /** AII-100: opening a chat with messages you hadn't seen shows a "New messages" line above them. */
    @Test fun newLineLight() {
        // A chat with no pictures, so the shot never catches one half loaded.
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }
        prime(c)
        val all = repo.store.cachedConversation(c.key)!!.bubbles
        val last = all.indexOfLast { !it.mine }
        repo.store.markSeen(c.key, java.time.Instant.parse(all[last].at).minusSeconds(1).toString())
        shot("chat_new_line", false) { ChatScreen(repo, c, onBack = {}) }
        rule.onNodeWithTag("new-line").assertExists()
    }
    /** AII-102: with more new messages than fit, the chat opens with the "New messages" line near the top. */
    @Config(sdk = [34], qualifiers = "w411dp-h480dp-xxhdpi")   // a short screen, so the new messages don't all fit
    @Test fun opensAtNewLine() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        val all = repo.store.cachedConversation(lead().key)!!.bubbles
        repo.store.markSeen(lead().key, java.time.Instant.parse(all.first().at).minusSeconds(1).toString())
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.waitForIdle()
        val line = rule.onNodeWithTag("new-line").fetchSemanticsNode().boundsInRoot
        val screen = rule.onRoot().fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("line at ${line.top} of ${screen.height}", line.top in 0f..screen.height / 3)
    }
    /** AII-105: a search result opens the chat at that message, even far up a long chat. */
    @Config(sdk = [34], qualifiers = "w411dp-h480dp-xxhdpi")
    @Test fun opensAtSearchResult() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        val first = repo.store.cachedConversation(lead().key)!!.bubbles.first()
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}, openAt = first.key) } }
        rule.waitForIdle()
        rule.onNodeWithText(first.text.lines().first().trim(), substring = true).assertIsDisplayed()
    }
    /** AII-117: while scrolling, the day floats at the top; it fades a little after you let go. */
    @Config(sdk = [34], qualifiers = "w411dp-h480dp-xxhdpi")
    @Test fun dayPillWhileScrolling() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+night")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("day-pill").assertDoesNotExist()
        rule.onRoot().performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(0f, 150f)); moveBy(androidx.compose.ui.geometry.Offset(0f, 150f)) }
        rule.waitForIdle()
        rule.onNodeWithTag("day-pill").assertIsDisplayed().assertTextEquals("Today")
        rule.onRoot().captureRoboImage("../docs/shots/chat_day_pill_dark.png")
        rule.onRoot().performTouchInput { up() }
        rule.mainClock.advanceTimeBy(2500)
        rule.waitForIdle()
        rule.onNodeWithTag("day-pill").assertDoesNotExist()
    }
    /** AII-104: hold a picture in a chat: Save and Share, without opening it. */
    @Test fun holdPicture() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onAllNodesWithTag("pic", useUnmergedTree = true).onFirst().performTouchInput { longClick() }
        rule.onNodeWithTag("pic-save").assertExists()
        rule.onNodeWithTag("pic-share").assertExists()
        rule.onAllNodesWithTag("full-pic").assertCountEquals(0)   // holding doesn't open it full screen
    }
    /** AII-101: drag a message to the right and let go: the reply bar shows its quote. */
    @Test fun swipeReplyLight() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }
        prime(c); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, c, onBack = {}) } }
        rule.onAllNodesWithTag("swipe-msg").onLast().performTouchInput { swipeRight(startX = centerX - 200f, endX = centerX + 200f) }
        rule.onNodeWithTag("quoting").assertExists()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("../docs/shots/chat_swipe_reply_light.png")
    }
    /** AII-99: reply to a message, then tap the quote: the quoted message gets outlined. */
    @Test fun quoteJumpLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onAllNodesWithTag("theirs").onLast().performTouchInput { longClick() }
        rule.onNodeWithTag("reply").performClick()
        rule.onNodeWithTag("input").performTextInput("Yes, thanks")
        rule.onNodeWithTag("send").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("quote", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.autoAdvance = false
        rule.onAllNodesWithTag("quote", useUnmergedTree = true).onFirst().performClick()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onRoot().captureRoboImage("../docs/shots/chat_quote_jump_light.png")
        rule.mainClock.autoAdvance = true
    }
    /** AII-127: hold a message, Select text: the message opens on its own, ready to select. */
    @Test fun selectTextLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onAllNodesWithTag("theirs").onLast().performTouchInput { longClick() }
        rule.onNodeWithTag("select").performClick()
        rule.onNodeWithTag("select-text").assertExists()
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chat_select_text_light.png")   // the dialog is its own window
        rule.onNodeWithText("Done").performClick()
        rule.onAllNodesWithTag("select-text").assertCountEquals(0)
    }
    /** AII-126: with Enter key sends on, the keyboard's Send key sends the message; off, it doesn't. */
    @Test fun enterSends() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }; prime(c)
        try {
            Appearance.enterSends = true
            rule.setContent { MessengerTheme { ChatScreen(repo, c, onBack = {}) } }
            val before = rule.onAllNodesWithTag("mine").fetchSemanticsNodes().size
            rule.onNodeWithTag("input").performTextInput("Sent with Enter")
            rule.onNodeWithTag("input").performImeAction()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("mine").fetchSemanticsNodes().size > before }
        } finally { Appearance.enterSends = false }
    }
    /** AII-146: hold Send, Send in 1 hour: it waits marked "Sends at"; tap the mark, Send now: it goes. */
    @Test fun sendLater() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }; prime(c); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, c, onBack = {}) } }
        rule.onNodeWithTag("input").performTextInput("Morning! Coffee later?")
        rule.onNodeWithTag("send").performTouchInput { longClick() }
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/send_later_menu_light.png")
        rule.onNodeWithTag("send-hour").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("later-mark").assertIsDisplayed()
        org.junit.Assert.assertEquals("2026-10-13T14:00:00Z", repo.pendingFor(c.key).single().sendAt)
        rule.onRoot().captureRoboImage("../docs/shots/send_later_light.png")
        // AII-150: Edit puts it back in the box and it stops waiting; hold Send again for a new time.
        rule.onNodeWithTag("later-mark").performClick()
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/send_later_edit_light.png")
        rule.onNodeWithTag("edit-later").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertTrue(repo.pendingFor(c.key).isEmpty())
        rule.onNodeWithTag("input").assertTextEquals("Morning! Coffee later?")
        rule.onNodeWithTag("input").performTextInput(" ☕")
        rule.onNodeWithTag("send").performTouchInput { longClick() }
        rule.onNodeWithTag("send-hour").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertEquals("Morning! Coffee later? ☕", repo.pendingFor(c.key).single().text)
        // AII-169: Change time opens the clock on 15:00 (London), and 18:30 moves it, still waiting.
        rule.onNodeWithTag("later-mark").performClick()
        rule.onNodeWithTag("change-time").performClick()
        rule.waitForIdle()
        val picker = org.robolectric.shadows.ShadowDialog.getLatestDialog() as android.app.TimePickerDialog
        org.junit.Assert.assertEquals(15, picker.onSaveInstanceState().getInt("hour"))
        picker.updateTime(18, 30)
        picker.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        rule.waitForIdle()
        org.junit.Assert.assertEquals("2026-10-13T17:30:00Z", repo.pendingFor(c.key).single().sendAt)
        rule.onNodeWithTag("later-mark").assertIsDisplayed()
        rule.onNodeWithTag("later-mark").performClick()
        rule.onNodeWithTag("send-now").performClick()
        rule.waitUntil(5_000) { repo.pendingFor(c.key).isEmpty() }
    }
    @Test fun enterSendsSetting() {
        rule.setContent { MessengerTheme { SettingsScreen(repo, {}, {}) } }
        rule.onNodeWithTag("enterSends").performScrollTo()
        rule.onNodeWithTag("enterSends").onChildren().filterToOne(androidx.compose.ui.test.isToggleable()).performClick()
        try { org.junit.Assert.assertTrue(repo.store.enterSends && Appearance.enterSends) } finally { Appearance.enterSends = false; repo.store.enterSends = false }
    }
    /** AII-122: on a short screen, scroll to the bottom of the chats, tap the title: back at the first chat. */
    @Test @Config(qualifiers = "w411dp-h300dp-xxhdpi") fun titleTapGoesToTop() {
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatsScreen(repo, {}, {}, {}) } }
        val first = "chat:${chats.first().key}"
        rule.onNodeWithTag("chats-list").performScrollToIndex(chats.size)
        rule.waitForIdle()
        org.junit.Assert.assertTrue(rule.onAllNodesWithTag(first).fetchSemanticsNodes().none { n -> n.boundsInRoot.bottom > 0f && n.boundsInRoot.top < rule.onRoot().fetchSemanticsNode().size.height })
        rule.onNodeWithTag("chats-title").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(first).assertIsDisplayed()
    }
    /** AII-121: hold a message, Share: the phone's share menu gets the message text. */
    @Test fun shareMessage() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onAllNodesWithTag("theirs").onLast().performTouchInput { longClick() }
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chat_share_menu_light.png")
        rule.onNodeWithTag("share").performClick()
        val chooser = org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
        org.junit.Assert.assertEquals(android.content.Intent.ACTION_CHOOSER, chooser.action)
        val send = chooser.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)!!
        org.junit.Assert.assertEquals("text/plain", send.type)
        org.junit.Assert.assertTrue(send.getStringExtra(android.content.Intent.EXTRA_TEXT).orEmpty().isNotBlank())
    }
    /** AII-113: with "working on AIG-2" showing, tapping the chat name still opens the details. */
    @Test fun workingOnKeepsTheNameTap() {
        prime(lead())
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("working-on", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(androidx.compose.ui.test.hasText("AI-gile"), useUnmergedTree = true).onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("details-pin").fetchSemanticsNode()
    }
    /** AII-128: chat details, Find in this chat: the sheet closes and the find bar opens. */
    @Test fun detailsFindOpensTheFindBar() {
        prime(lead())
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onAllNodes(androidx.compose.ui.test.hasText("AI-gile"), useUnmergedTree = true).onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("details-find").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("chat-details").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithContentDescription("Close find").assertExists()
    }
    /** AII-80: tap the chat name; the details sheet opens (dark, as the phone shows it at night). */
    @Test fun chatDetailsDark() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+night")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onNodeWithTag("chat-title").performClick()
        rule.onNodeWithTag("details-pin").fetchSemanticsNode()
        rule.onNodeWithTag("details-card-AIG-2").performClick()   // AII-85: the open card unfolds in the sheet
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("card-details").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        rule.onAllNodes(androidx.compose.ui.test.isRoot()).onLast().captureRoboImage("../docs/shots/chat_details_dark.png")   // the sheet is its own window
    }
    /** AII-160: a 1 hour mute shows when it ends under Mute alerts in the chat details. */
    @Test fun muteEndsLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onNodeWithTag("chat-title").performClick()
        rule.onNodeWithTag("details-mute").performClick()
        rule.onNodeWithTag("mute-for:HOUR").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("details-mute-ends").assertExists()
        rule.onAllNodes(androidx.compose.ui.test.isRoot()).onLast().captureRoboImage("../docs/shots/chat_details_mute_ends_light.png")   // the sheet is its own window
        rule.onNodeWithTag("details-mute").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertTrue(rule.onAllNodesWithTag("details-mute-ends").fetchSemanticsNodes().isEmpty())
    }
}
