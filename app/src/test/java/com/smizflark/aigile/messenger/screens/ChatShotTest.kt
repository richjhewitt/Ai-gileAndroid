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
import androidx.compose.ui.test.swipeUp
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
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
    /** AII-375: a chat with its own colour (purple) tints your bubbles and the top bar. */
    @Test fun chatColourLight() { prime(lead()); repo.store.chatColours = com.smizflark.aigile.messenger.data.ChatColour.put(emptySet(), lead().key, "purple"); shot("chat_colour", false) { ChatScreen(repo, lead(), onBack = {}) } }
    @Test fun chatColourDark() { prime(lead()); repo.store.chatColours = com.smizflark.aigile.messenger.data.ChatColour.put(emptySet(), lead().key, "purple"); shot("chat_colour", true) { ChatScreen(repo, lead(), onBack = {}) } }
    /** AII-391: Wi-Fi only on mobile data: a picture waits behind a "Tap to load" box (light); a tap loads it. */
    @Test fun tapToLoadLight() {
        com.smizflark.aigile.messenger.data.PicLoad.meteredOverride = true
        Appearance.loadPictures = com.smizflark.aigile.messenger.data.PicLoad.WIFI; repo.store.loadPictures = Appearance.loadPictures
        try {
            val a = com.smizflark.aigile.messenger.data.Bubble("p1", "Lead", "Here is the new logo https://media.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif", "2026-10-14T09:00:00Z", mine = false)
            val b = com.smizflark.aigile.messenger.data.Bubble("p2", "Me", "Nice, thanks!", "2026-10-14T09:01:00Z", mine = true)
            shot("chat_tap_to_load", false) {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().padding(top = 48.dp)) {
                    listOf(a, b).forEach { m ->
                        com.smizflark.aigile.messenger.ui.MessageBubble(com.smizflark.aigile.messenger.ui.Row.Msg(m.key, m), lead(), repo, 0, onReply = {}, onForward = {}, cards = emptySet(), onCard = {},
                            onOption = {}, onRetry = {}, onDiscard = {})
                    }
                }
            }
            rule.onNodeWithTag("pic-blocked").assertIsDisplayed()
            rule.onNodeWithTag("pic-blocked").performClick()
            rule.waitForIdle()
            rule.onAllNodesWithTag("pic-blocked").assertCountEquals(0)
        } finally { com.smizflark.aigile.messenger.data.PicLoad.meteredOverride = null; Appearance.loadPictures = "always" }
    }
    @Test fun codeBoxDark() { val t = chats.first { it.id == "tunio" }; prime(t); shot("chat_code", true) { ChatScreen(repo, t, onBack = {}) } }   // AII-115
    @Test fun chatShowTimes() {   // AII-211
        prime(lead()); Appearance.showTimes = true
        try { shot("chat_times", false) { ChatScreen(repo, lead(), onBack = {}) } } finally { Appearance.showTimes = false }
    }
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
    /** AII-339: two pinned messages show the pinned bar, "1 of 2". */
    @Test fun pinnedBarLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        val theirs = repo.store.cachedConversation(lead().key)!!.bubbles.filter { !it.mine && it.text.isNotBlank() }
        repo.store.setPinnedMsgs(theirs.take(2).mapIndexed { i, b -> com.smizflark.aigile.messenger.data.PinnedMsgs.of(lead().key, b, 10L + i) })
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("pinned-bar").assertIsDisplayed()
        rule.onRoot().captureRoboImage("../docs/shots/chat_pinned_light.png")
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
    /** AII-371: hold a message, Select, tap a second one: select mode with 2 picked. */
    @Test fun selectModeLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onAllNodesWithTag("theirs").onLast().performTouchInput { longClick() }
        rule.onNodeWithTag("select-msg").performClick()
        rule.onAllNodesWithTag("mine").onLast().performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("select-count").assertTextEquals("2 selected")
        rule.onRoot().captureRoboImage("../docs/shots/chat_select_light.png")
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
    /** AII-174: clock, Repeat every day: the bubble says 🔁; Send now sends it and tomorrow's waits at the same time. */
    @Test fun sendLaterRepeatsLight() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }; prime(c); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, c, onBack = {}) } }
        rule.onNodeWithTag("input").performTextInput("Daily stand-up in 10")
        rule.onNodeWithTag("send").performTouchInput { longClick() }
        rule.onNodeWithTag("send-hour").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("later-mark").performClick()
        rule.onNodeWithTag("repeat-day").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertEquals("day", repo.pendingFor(c.key).single().repeat)
        rule.onNodeWithText("Sends at 15:00 🔁").assertIsDisplayed()
        rule.onRoot().captureRoboImage("../docs/shots/send_later_repeat_light.png")
        rule.onNodeWithTag("later-mark").performClick()
        rule.onNodeWithTag("send-now").performClick()
        rule.waitUntil(5_000) { repo.pendingFor(c.key).singleOrNull()?.sendAt == "2026-10-14T14:00:00Z" }
        org.junit.Assert.assertEquals("Daily stand-up in 10", repo.pendingFor(c.key).single().text)
    }
    /** AII-170: hold Send, Pick a day…: calendar, then clock; the bubble says the day. */
    @Test fun sendLaterOnADay() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }; prime(c); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, c, onBack = {}) } }
        rule.onNodeWithTag("input").performTextInput("Friday, then?")
        rule.onNodeWithTag("send").performTouchInput { longClick() }
        rule.onNodeWithTag("send-day").performClick()
        rule.waitForIdle()
        val cal = org.robolectric.shadows.ShadowDialog.getLatestDialog() as android.app.DatePickerDialog
        cal.updateDate(2026, 9, 16)
        cal.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        rule.waitForIdle()
        val clock = org.robolectric.shadows.ShadowDialog.getLatestDialog() as android.app.TimePickerDialog
        clock.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        rule.waitForIdle()
        org.junit.Assert.assertEquals("2026-10-16T08:00:00Z", repo.pendingFor(c.key).single().sendAt)
        rule.onNodeWithText("Sends at Fri 09:00", substring = true).assertExists()
        rule.onRoot().captureRoboImage("../docs/shots/send_later_day_light.png")
    }
    @Test fun enterSendsSetting() {
        rule.setContent { MessengerTheme { SettingsScreen(repo, {}, {}) } }
        rule.onNodeWithTag("enterSends").performScrollTo()
        rule.onNodeWithTag("enterSends").onChildren().filterToOne(androidx.compose.ui.test.isToggleable()).performClick()
        try { org.junit.Assert.assertTrue(repo.store.enterSends && Appearance.enterSends) } finally { Appearance.enterSends = false; repo.store.enterSends = false }
    }
    /** AII-179: an empty box shows the quick replies; a tap puts one in the box (no send), and they hide while there is text. */
    @Test fun quickReplies() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }; prime(c); RuntimeEnvironment.setQualifiers("+notnight")
        try {
            Appearance.quickReplies = true
            rule.setContent { MessengerTheme { ChatScreen(repo, c, onBack = {}) } }
            rule.waitForIdle()
            rule.onRoot().captureRoboImage("../docs/shots/quick_replies_light.png")
            val before = rule.onAllNodesWithTag("mine").fetchSemanticsNodes().size
            rule.onNodeWithText("Not yet").performClick()
            rule.onNodeWithTag("input").assertTextEquals("Not yet", includeEditableText = true)
            rule.onAllNodesWithTag("quick-replies").assertCountEquals(0)
            org.junit.Assert.assertEquals(before, rule.onAllNodesWithTag("mine").fetchSemanticsNodes().size)
        } finally { Appearance.quickReplies = false }
    }
    @Test fun quickRepliesSetting() {
        rule.setContent { MessengerTheme { SettingsScreen(repo, {}, {}) } }
        rule.onNodeWithTag("quickReplies").performScrollTo()
        org.junit.Assert.assertTrue(Appearance.quickReplies)   // on to start with
        rule.onNodeWithTag("quickReplies").onChildren().filterToOne(androidx.compose.ui.test.isToggleable()).performClick()
        try { org.junit.Assert.assertFalse(repo.store.quickReplies || Appearance.quickReplies) } finally { Appearance.quickReplies = false; repo.store.quickReplies = true }
    }
    /** AII-180: type your own replies in Settings; the chat shows them; clear the box and the starting ones are back. */
    @Test fun ownQuickReplies() {
        val c = chats.first { it.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND }; prime(c)
        var settings by androidx.compose.runtime.mutableStateOf(true)
        rule.setContent { MessengerTheme { if (settings) SettingsScreen(repo, {}, {}) else ChatScreen(repo, c, onBack = {}) } }
        try {
            rule.onNodeWithTag("quickReplyText").performScrollTo().performTextReplacement("On my way\n\nBack at 5")
            org.junit.Assert.assertEquals(listOf("On my way", "Back at 5"), Appearance.replies)
            settings = false; rule.waitForIdle()
            rule.onNodeWithText("Back at 5").performClick()
            rule.onNodeWithTag("input").assertTextEquals("Back at 5", includeEditableText = true)
            rule.onAllNodesWithText("Not yet").assertCountEquals(0)
            settings = true; rule.waitForIdle()
            rule.onNodeWithTag("quickReplyText").performScrollTo().performTextReplacement("")
            org.junit.Assert.assertEquals(emptyList<String>(), Appearance.replies)
        } finally { Appearance.quickReplies = false; Appearance.replies = emptyList(); repo.store.quickReplyText = "" }
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
    /** AII-193: the chat's menu lists Go to date; tapping it opens the date picker. */
    @Test fun goToDateMenuLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onNodeWithContentDescription("More").performClick()
        rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chat_go_to_date_menu_light.png")
        rule.onNodeWithTag("go-to-date").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Cancel").assertIsDisplayed()
    }
    /** AII-368: the chat menu shows Add to home screen. */
    @Test fun addToHomeMenuLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onNodeWithContentDescription("More").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("add-home").assertIsDisplayed()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chat_add_to_home_menu_light.png")
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
    /** AII-372: hold a message, Info: the exact sent time. */
    @Test fun messageInfoLight() {
        prime(lead()); RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}) } }
        rule.onAllNodesWithTag("theirs").onLast().performTouchInput { longClick() }
        rule.waitForIdle()
        rule.onNodeWithTag("info").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("info-sent").assertIsDisplayed()
        com.github.takahirom.roborazzi.captureScreenRoboImage("../docs/shots/chat_message_info_light.png")
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
    /** AII-273: the demo's test card with Accept and Send back; Accept moves it to Done and the buttons go. */
    private fun testCard(dark: Boolean) {
        val t = chats.first { it.id == "tunio" }; prime(t)
        show(dark) { ChatScreen(repo, t, onBack = {}, focus = "TUN-7") }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("card-accept").fetchSemanticsNodes().isNotEmpty() }
        rule.onRoot().captureRoboImage("../docs/shots/card_test_${if (dark) "dark" else "light"}.png")
        rule.onNodeWithTag("card-accept").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("card-accept").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Done · Lead").assertExists()
    }
    /** AII-282: a busy card waiting for your test opens at its steps, so Accept is on screen. */
    @Test fun busyCardToTestOpensAtItsSteps() {
        val t = chats.first { it.id == "tunio" }; prime(t)
        repeat(15) { repo.queue(t, "Another note on the export, number $it", "TUN-7") }
        show(false) { ChatScreen(repo, t, onBack = {}, focus = "TUN-7") }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("card-accept").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        rule.onNodeWithTag("card-accept").assertIsDisplayed()
    }
    @Test fun testCardLight() = testCard(false)
    @Test fun testCardDark() = testCard(true)
    /** AII-219: tapping "working on AIG-2" opens that card, like a card link. */
    @Test fun workingOnTapOpensTheCard() {
        prime(lead())
        var opened: String? = null
        rule.setContent { MessengerTheme { ChatScreen(repo, lead(), onBack = {}, onOpenCard = { opened = it }) } }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("working-on", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithTag("working-on", useUnmergedTree = true).onFirst().performClick()
        rule.waitForIdle()
        org.junit.Assert.assertNotNull(opened)
        org.junit.Assert.assertTrue(opened!!.isNotBlank())
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
    /** AII-194: the Links list in the chat details (named and plain links). */
    @Test fun chatDetailsLinksLight() {
        RuntimeEnvironment.setQualifiers("+notnight")
        val links = com.smizflark.aigile.messenger.data.Links.of(listOf("see https://github.com/smizflark/aigile/pull/194.", "[Release notes](https://example.org/notes) and dnbloco.com", "[Board](https://aigile.smizflark.com/board)"))
        rule.setContent { MessengerTheme { com.smizflark.aigile.messenger.ui.ChatDetails(lead(), repo, com.smizflark.aigile.messenger.data.ChatInfo(emptyList(), emptyList(), links), onClose = {}) } }
        rule.waitForIdle()
        rule.onAllNodesWithTag("details-link").assertCountEquals(4)
        rule.onNodeWithTag("chat-details").performTouchInput { swipeUp() }   // the sheet opens half way: pull it up so every link shows
        rule.waitForIdle()
        rule.onAllNodes(androidx.compose.ui.test.isRoot()).onLast().captureRoboImage("../docs/shots/chat_details_links_light.png")
    }
    /** AII-197: the Files list in the chat details. */
    @Test fun chatDetailsFilesLight() {
        RuntimeEnvironment.setQualifiers("+notnight")
        val files = listOf(com.smizflark.aigile.messenger.data.Pic("/api/f1", "Invoice-October.pdf", "application/pdf"), com.smizflark.aigile.messenger.data.Pic("/api/f2", "notes.txt"), com.smizflark.aigile.messenger.data.Pic("/api/f3", "export.csv", "text/csv"))
        rule.setContent { MessengerTheme { com.smizflark.aigile.messenger.ui.ChatDetails(lead(), repo, com.smizflark.aigile.messenger.data.ChatInfo(emptyList(), emptyList(), emptyList(), files), onClose = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("chat-details").performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onAllNodesWithTag("details-file").assertCountEquals(3)
        rule.onAllNodes(androidx.compose.ui.test.isRoot()).onLast().captureRoboImage("../docs/shots/chat_details_files_light.png")
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
