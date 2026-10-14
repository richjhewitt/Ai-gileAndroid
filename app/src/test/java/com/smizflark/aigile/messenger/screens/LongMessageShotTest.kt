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

/** AII-184: a long message folds to its first lines with Read more; short ones and find matches show in full. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class LongMessageShotTest : ShotBase() {
    private val long = Bubble("long", "Lead", (1..30).joinToString("\n") { "- Line $it of the plan" }, "2026-10-14T09:00:00Z", mine = false)
    private val short = long.copy(key = "short", text = "- Line 1 of the plan\n- Line 2 of the plan")

    private fun show(b: Bubble, mark: Int = 0) {
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent {
            MessengerTheme {
                MessageBubble(Row.Msg(b.key, b), lead(), repo, mark, onReply = {}, onForward = {}, cards = emptySet(), onCard = {},
                    onOption = {}, onRetry = {}, onDiscard = {})
            }
        }
        rule.waitForIdle()
    }

    @Test fun foldsAndOpens() {
        show(long)
        rule.onNodeWithText("Read more").assertExists()
        rule.onRoot().captureRoboImage("../docs/shots/chat_read_more_light.png")
        rule.onNodeWithText("Read more").performClick()
        rule.onNodeWithText("Show less").assertExists()
        rule.onNodeWithText("Show less").performClick()
        rule.onNodeWithText("Read more").assertExists()
    }

    @Test fun shortMessageHasNoButton() { show(short); rule.onNodeWithTag("read-more").assertDoesNotExist() }

    @Test fun findMatchShowsInFull() { show(long, mark = 2); rule.onNodeWithTag("read-more").assertDoesNotExist() }
}
