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

/** AII-191: a **Label:** value line gets a ⧉ to copy the value. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class CopyValueShotTest : ShotBase() {
    private val msg = Bubble("copyvalue", "Lead", "Here are the details:\n\n**Username:** rich\n\n**Password:** abc123\n\n**Command:** `npm run build`\n\n**Note:** two words, no mark", "2026-10-14T09:00:00Z", mine = false)

    @Test fun showsACopyMark() {
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent {
            MessengerTheme {
                MessageBubble(Row.Msg(msg.key, msg), lead(), repo, 0, onReply = {}, onForward = {}, cards = emptySet(), onCard = {},
                    onOption = {}, onRetry = {}, onDiscard = {})
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Password", substring = true).assertExists()
        rule.onRoot().captureRoboImage("../docs/shots/chat_copy_value_light.png")
    }

    @Test fun showsACopyMarkInDark() {
        RuntimeEnvironment.setQualifiers("+night")
        rule.setContent {
            MessengerTheme {
                MessageBubble(Row.Msg(msg.key, msg), lead(), repo, 0, onReply = {}, onForward = {}, cards = emptySet(), onCard = {},
                    onOption = {}, onRetry = {}, onDiscard = {})
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Password", substring = true).assertExists()
        rule.onRoot().captureRoboImage("../docs/shots/chat_copy_value_dark.png")
    }
}
