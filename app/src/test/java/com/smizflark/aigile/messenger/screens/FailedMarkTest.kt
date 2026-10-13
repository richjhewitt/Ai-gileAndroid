package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.Outbox
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.MessengerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-62: a message that failed shows a red mark; tapping it offers Retry and Delete. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class FailedMarkTest {
    @get:Rule val rule = createComposeRule()

    @Test fun markThenDelete() {
        val repo = Repo(ApplicationProvider.getApplicationContext<Application>())
        repo.startDemo()
        val ref = runBlocking { repo.chats() }.first { it.id == "ai-gile" }
        runBlocking { repo.conversation(ref) }
        repo.store.saveOutbox(Outbox.fail(listOf(Pending("p1", ref.key, "stuck message", null, "2026-10-13T10:00:00Z")), "p1", offline = false))
        rule.setContent { MessengerTheme { ChatScreen(repo, ref, onBack = {}) } }
        rule.waitForIdle()
        rule.onNodeWithTag("failed-mark").assertIsDisplayed()
        rule.onNodeWithTag("failed-mark").performClick()
        rule.onNodeWithTag("retry").assertIsDisplayed()
        rule.onNodeWithTag("delete").assertIsDisplayed().performClick()
        rule.waitForIdle()
        assertEquals(0, repo.pendingFor(ref.key).size)
    }
}
