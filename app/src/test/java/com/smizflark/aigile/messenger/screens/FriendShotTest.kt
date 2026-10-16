package com.smizflark.aigile.messenger.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.MessengerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-324: a friend chat with an edited line, a deleted line and a reply; and its details with the safety code. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class FriendShotTest : ShotBase() {
    private fun davy() = chats.first { it.kind == Kind.FRIEND }

    @Test fun davyEditedDeletedReplyDark() {
        runBlocking {
            val s = repo.source
            s.edit(davy(), "d2", "After the barbers, yes")
            s.send(davy(), "Sounds good, see you at 7", null, emptyList(), "d1")
            s.send(davy(), "Oops, wrong chat", null, emptyList())
            val oops = s.conversation(davy()).bubbles.last().id!!
            s.delete(davy(), oops)
        }
        prime(davy())
        shot("chat_friend", true) { ChatScreen(repo, davy(), onBack = {}) }
        rule.onAllNodesWithTag("deleted", useUnmergedTree = true).assertCountEquals(1)   // the bubble merges its parts
        rule.onAllNodesWithTag("quote", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test fun davyReactsDark() {
        runBlocking { repo.source.react(davy(), "d1", "👍"); repo.source.react(davy(), "d1", "❤️") }
        prime(davy())
        shot("chat_friend_reacts", true) { ChatScreen(repo, davy(), onBack = {}) }
        rule.onAllNodesWithTag("react-chip", useUnmergedTree = true).assertCountEquals(2)
    }

    @Test fun davyDetailsSafetyCodeDark() {
        prime(davy()); RuntimeEnvironment.setQualifiers("+night")
        rule.setContent { MessengerTheme { ChatScreen(repo, davy(), onBack = {}) } }
        rule.onNodeWithTag("chat-title").performClick()
        rule.onNodeWithTag("we-checked-it").assertIsDisplayed()
        rule.waitForIdle()
        rule.onAllNodes(androidx.compose.ui.test.isRoot()).onLast().captureRoboImage("../docs/shots/chat_friend_details_dark.png")   // the sheet is its own window
        rule.onNodeWithTag("we-checked-it").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("safety-checked").assertIsDisplayed()
    }
}
