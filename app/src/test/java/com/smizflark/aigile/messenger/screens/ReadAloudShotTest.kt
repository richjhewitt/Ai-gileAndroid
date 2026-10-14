package com.smizflark.aigile.messenger.screens

import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.ui.MessageBubble
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.ReadAloud
import com.smizflark.aigile.messenger.ui.Row
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-185: hold a message, Read aloud; the same item then says Stop reading. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ReadAloudShotTest : ShotBase() {
    private val msg = Bubble("hello", "Lead", "Your haircut is at **three**", "2026-10-14T09:00:00Z", mine = false)

    @After fun quiet() { ReadAloud.stop() }

    @Test fun readsAndStops() {
        RuntimeEnvironment.setQualifiers("+notnight")
        rule.setContent {
            MessengerTheme {
                MessageBubble(Row.Msg(msg.key, msg), lead(), repo, 0, onReply = {}, onForward = {}, cards = emptySet(), onCard = {},
                    onOption = {}, onRetry = {}, onDiscard = {})
            }
        }
        rule.onNodeWithTag("theirs").performTouchInput { longClick() }
        rule.onNodeWithText("Read aloud").performClick()
        assertEquals("hello", ReadAloud.speaking)
        rule.onNodeWithTag("theirs").performTouchInput { longClick() }
        rule.onNodeWithText("Stop reading").performClick()
        assertNull(ReadAloud.speaking)
    }
}
