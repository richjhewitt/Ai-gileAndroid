package com.smizflark.aigile.messenger.screens

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.ui.MessageBubble
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.Row
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-187: a [name](link) shows its name, a bare web address is tappable, • lines are bullets. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class LinksShotTest : ShotBase() {
    private val msg = Bubble("links", "Lead", "- Read [the guide](https://dnbloco.com/guide) first\n\n- Then open dnbloco.com and check it loads.\n\n• Live at https://smizflark.com/a.", "2026-10-14T09:00:00Z", mine = false)

    @Test fun showsLinksLikeTheBoard() {
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent {
            MessengerTheme {
                MessageBubble(Row.Msg(msg.key, msg), lead(), repo, 0, onReply = {}, onForward = {}, cards = emptySet(), onCard = {},
                    onOption = {}, onRetry = {}, onDiscard = {})
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("the guide", substring = true).assertExists()
        rule.onRoot().captureRoboImage("../docs/shots/chat_links_light.png")
    }
}
