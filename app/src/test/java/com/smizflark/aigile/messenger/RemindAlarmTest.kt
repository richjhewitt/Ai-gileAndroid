package com.smizflark.aigile.messenger

import android.app.AlarmManager
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Remind
import com.smizflark.aigile.messenger.data.RemindAlarm
import com.smizflark.aigile.messenger.data.Reminder
import com.smizflark.aigile.messenger.work.RemindWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RemindAlarmTest {
    @org.junit.Before fun workManager() {
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true)
        runCatching { androidx.work.WorkManager.initialize(ApplicationProvider.getApplicationContext(), androidx.work.Configuration.Builder().setExecutor { it.run() }.build()) }
    }

    private val r = Reminder("c|m", "c", "Chat", "m", "hello", System.currentTimeMillis() + 3_600_000)

    @Test fun exactBelowAndroid12OrWhenAllowed() {
        assertTrue(RemindAlarm.useExact(30, false))
        assertTrue(RemindAlarm.useExact(34, true))
        assertFalse(RemindAlarm.useExact(34, false))
    }

    @Test fun asksOnlyOnceAndOnlyWhenNotAllowed() {
        assertTrue(RemindAlarm.shouldAsk(34, false, false))
        assertFalse(RemindAlarm.shouldAsk(34, false, true))
        assertFalse(RemindAlarm.shouldAsk(34, true, false))
        assertFalse(RemindAlarm.shouldAsk(30, false, false))
    }

    @Test fun reschedulesOnlyWhatIsStillToCome() {
        val past = r.copy(id = "p", at = 100)
        val future = r.copy(id = "f", at = 900)
        assertEquals(listOf(future), RemindAlarm.toReschedule(listOf(past, future), 500))
    }

    @Test fun alarmIsSetAtTheRightTimeAndCancelled() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val am = shadowOf(ctx.getSystemService(AlarmManager::class.java))
        RemindWorker.schedule(ctx, r)
        val a = am.scheduledAlarms.single()
        assertEquals(r.at, a.triggerAtTime)
        assertEquals(AlarmManager.RTC_WAKEUP, a.type)
        RemindWorker.cancel(ctx, r.id)
        assertTrue(am.scheduledAlarms.isEmpty())
    }

    @Test fun schedulingAgainReplacesTheAlarm() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val am = shadowOf(ctx.getSystemService(AlarmManager::class.java))
        RemindWorker.schedule(ctx, r)
        RemindWorker.schedule(ctx, r.copy(at = r.at + 60_000))
        assertEquals(r.at + 60_000, am.scheduledAlarms.single().triggerAtTime)
    }

    @Test fun withoutPermissionNoAlarmIsSet() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val am = shadowOf(ctx.getSystemService(AlarmManager::class.java))
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(false)
        RemindWorker.schedule(ctx, r)
        assertTrue(am.scheduledAlarms.isEmpty())
    }
}
