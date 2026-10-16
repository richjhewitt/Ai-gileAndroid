package com.smizflark.aigile.messenger.screens

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.LinkPreview
import com.smizflark.aigile.messenger.ui.Appearance
import com.smizflark.aigile.messenger.ui.MessageBubble
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.Row
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-341: a bubble with a web link gets a card with the page title (fake page, no network). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class LinkPreviewShotTest : ShotBase() {
    private val url = "https://smizflark.com/guide"
    private val msg = Bubble("preview", "Lead", "The guide is here: $url", "2026-10-14T09:00:00Z", mine = false)

    @After fun off() { Appearance.linkPreviews = false }

    @Test fun showsCardUnderBubble() {
        RuntimeEnvironment.setQualifiers("+notnight")
        LinkPreview.seed(LinkPreview(url, "The Smizflark guide: getting started", null))
        Appearance.linkPreviews = true
        rule.setContent {
            MessengerTheme {
                MessageBubble(Row.Msg(msg.key, msg), lead(), repo, 0, onReply = {}, onForward = {}, cards = emptySet(), onCard = {},
                    onOption = {}, onRetry = {}, onDiscard = {})
            }
        }
        // the card loads off the main thread, so wait for it rather than for idle (flaky in the full suite)
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("link-preview").fetchSemanticsNodes().isNotEmpty() }
        rule.onRoot().captureRoboImage("../docs/shots/chat_link_preview_light.png")
    }
}
