package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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

    /** AII-265: Skip saves the reminder and tells the bubble, so its bell line shows straight away. */
    @Test fun remindNoteSkipTellsBubble() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        androidx.work.WorkManager.initialize(app, androidx.work.Configuration.Builder().setExecutor { it.run() }.build())
        val repo = Repo(app).apply { startDemo() }
        val ref = runBlocking { repo.chats() }.first()
        val b = Bubble("m1", "Lead", "Ship it", "2026-10-12T09:00:00Z", mine = false)
        var saved = 0
        rule.setContent { MessengerTheme { RemindNoteDialog(4_000_000_000_000L, ref, b, AnnotatedString("Ship it"), repo, "", {}, onSaved = { saved++ }) {} } }
        rule.onNodeWithTag("remind-note-skip").performClick()
        assertEquals(1, saved)
        assertEquals(1, repo.store.reminders().size)
    }
}
