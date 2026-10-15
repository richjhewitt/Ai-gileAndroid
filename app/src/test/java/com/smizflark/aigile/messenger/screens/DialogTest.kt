package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.smizflark.aigile.messenger.data.Reminder
import com.smizflark.aigile.messenger.ui.ChangeBoard
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.RemindNoteDialog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Dialogs with a text field, tapped like a user. No @GraphicsMode(NATIVE) here on purpose:
 * under native graphics a text field in a dialog never lets Compose go idle, so these can't sit in the shot tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DialogTest {
    @get:Rule val rule = createComposeRule()

    private fun initWork(app: Application) {
        runCatching { androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().setExecutor { it.run() }.build()) }   // already set up by an earlier test
    }

    /** AII-265: Skip saves the reminder and tells the bubble, so its bell line shows straight away. */
    @Test fun remindNoteSkipTellsBubble() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        initWork(app)
        val repo = Repo(app).apply { startDemo() }
        val ref = runBlocking { repo.chats() }.first()
        val b = Bubble("m1", "Lead", "Ship it", "2026-10-12T09:00:00Z", mine = false)
        var saved = 0
        rule.setContent { MessengerTheme { RemindNoteDialog(4_000_000_000_000L, ref, b, AnnotatedString("Ship it"), repo, "", {}, onSaved = { saved++ }) {} } }
        rule.onNodeWithTag("remind-note-skip").performClick()
        assertEquals(1, saved)
        assertEquals(1, repo.store.reminders().size)
    }

    private fun remindWith(typed: String): Reminder {
        val app = ApplicationProvider.getApplicationContext<Application>()
        initWork(app)
        val repo = Repo(app).apply { startDemo() }
        val ref = runBlocking { repo.chats() }.first()
        val b = Bubble("m1", "Lead", "Ship it", "2026-10-12T09:00:00Z", mine = false)
        rule.setContent {
            MessengerTheme {
                var note by remember { mutableStateOf("") }
                RemindNoteDialog(4_000_000_000_000L, ref, b, AnnotatedString("Ship it"), repo, note, { note = it }) {}
            }
        }
        rule.onNodeWithTag("remind-note").performTextInput(typed)
        rule.onNodeWithTag("remind-note-save").performClick()
        return repo.store.reminders().single()
    }

    /** AII-244: type a note, tap Save, and the reminder keeps it. */
    @Test fun remindNoteSaveKeepsTypedNote() {
        assertEquals("Call Sam first", remindWith("Call Sam first").note)
    }

    /** AII-244: a note of only spaces is no note. */
    @Test fun remindNoteSaveDropsBlankNote() {
        assertNull(remindWith("   ").note)
    }

    private fun changeBoard(): Repo {
        val repo = Repo(ApplicationProvider.getApplicationContext<Application>()).apply { startDemo() }
        rule.setContent { MessengerTheme { Column { ChangeBoard(repo) {} } } }
        rule.onNodeWithTag("change").performClick()
        return repo
    }

    /** Change board: Save with no address says so and changes nothing. */
    @Test fun changeBoardBlankAddressShowsError() {
        val repo = changeBoard()
        rule.onNodeWithTag("url").performTextClearance()
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithTag("error").assertIsDisplayed()
        assertTrue(repo.store.demo)
    }

    /** AII-273: Send back needs a reason, then hands it on. */
    @Test fun sendBackAsksForAReason() {
        var sent: String? = null
        rule.setContent { MessengerTheme { com.smizflark.aigile.messenger.ui.SendBackDialog(onSend = { sent = it }, onDismiss = {}) } }
        rule.onNodeWithTag("send-back-go").assertIsNotEnabled()
        rule.onNodeWithTag("send-back-reason").performTextInput("  Export is empty ")
        rule.onNodeWithTag("send-back-go").performClick()
        assertEquals("Export is empty", sent)
    }

    /** Change board: Cancel after typing leaves the saved board alone and closes the form. */
    @Test fun changeBoardCancelKeepsBoard() {
        val repo = changeBoard()
        val before = repo.store.url
        rule.onNodeWithTag("url").performTextInput("other-pc.example.ts.net")
        rule.onNodeWithTag("key").performTextInput("secret")
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithTag("change").assertIsDisplayed()
        assertEquals(before, repo.store.url)
        assertTrue(repo.store.demo)
    }
}
