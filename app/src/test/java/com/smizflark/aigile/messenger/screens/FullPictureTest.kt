package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.ui.MessagePictures
import com.smizflark.aigile.messenger.ui.MessengerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-87: tap a picture, it opens full screen; swipe to the next one. Files that aren't pictures are skipped. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class FullPictureTest {
    @get:Rule val rule = createComposeRule()

    @Test fun swipeToNext() {
        val repo = Repo(ApplicationProvider.getApplicationContext<Application>())
        val pics = listOf(Pic("file:///a.png", "a.png"), Pic("file:///notes.pdf", "notes.pdf"), Pic("file:///b.jpg", "b.jpg"))
        rule.setContent { MessengerTheme { MessagePictures(pics, repo) } }
        rule.onAllNodesWithTag("pic").onFirst().performClick()
        rule.onNodeWithTag("full-pic-count", useUnmergedTree = true).assertTextEquals("1 / 2")
        rule.onNodeWithTag("full-pic-pager", useUnmergedTree = true).performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("full-pic-count", useUnmergedTree = true).assertTextEquals("2 / 2")
    }

    /** AII-90: double tap zooms in, so a drag looks around instead of swiping; double tap again zooms out. Tap closes. */
    @Test fun doubleTapZooms() {
        val repo = Repo(ApplicationProvider.getApplicationContext<Application>())
        val pics = listOf(Pic("file:///a.png", "a.png"), Pic("file:///b.jpg", "b.jpg"))
        rule.setContent { MessengerTheme { MessagePictures(pics, repo) } }
        rule.onAllNodesWithTag("pic").onFirst().performClick()
        val pager = rule.onNodeWithTag("full-pic-pager", useUnmergedTree = true)
        pager.performTouchInput { doubleClick() }
        rule.waitForIdle()
        pager.performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("full-pic-count", useUnmergedTree = true).assertTextEquals("1 / 2")
        pager.performTouchInput { doubleClick() }
        rule.waitForIdle()
        pager.performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("full-pic-count", useUnmergedTree = true).assertTextEquals("2 / 2")
        pager.performTouchInput { click() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.onNodeWithTag("full-pic", useUnmergedTree = true).assertDoesNotExist()
    }
}
