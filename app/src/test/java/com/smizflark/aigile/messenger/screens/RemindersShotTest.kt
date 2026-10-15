package com.smizflark.aigile.messenger.screens

import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Reminder
import com.smizflark.aigile.messenger.ui.RemindersScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-269: the Reminders list under Today, Tomorrow, Later, on the fixed clock. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class RemindersShotTest : ShotBase() {
    private fun seed() {
        val h = 3_600_000L
        val now = Now.millis()
        repo.store.setReminders(listOf(
            Reminder("a", "ai-gile", "AI-gile", "m1", "Check the build before the demo", now + h),
            Reminder("b", "ai-gile", "AI-gile", "m2", "Call back about the invoice", now + 2 * h),
            Reminder("c", "ai-gile", "AI-gile", "m3", "Morning stand-up notes", now + 20 * h),
            Reminder("d", "ai-gile", "AI-gile", "m4", "Send the release notes", now + 72 * h),
        ))
    }

    @Test fun remindersLight() { seed(); shot("reminders_groups", false) { RemindersScreen(repo, { _, _ -> }, {}) } }
    @Test fun remindersDark() { seed(); shot("reminders_groups", true) { RemindersScreen(repo, { _, _ -> }, {}) } }
}
