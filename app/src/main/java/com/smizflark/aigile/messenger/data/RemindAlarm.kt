package com.smizflark.aigile.messenger.data

/** Reminders on the minute (AII-396): the pure choices behind the exact alarm. */
object RemindAlarm {
    /** Exact alarm when the phone allows it (below Android 12 always); otherwise today's WorkManager path. */
    fun useExact(sdk: Int, canScheduleExact: Boolean) = sdk < 31 || canScheduleExact

    /** Ask "Let reminders go off on time?" once, only when exact alarms are not allowed and we have not asked before. */
    fun shouldAsk(sdk: Int, canScheduleExact: Boolean, asked: Boolean) = !useExact(sdk, canScheduleExact) && !asked

    /** After a reboot or an update the alarms are gone: every pending reminder is scheduled again. */
    fun toReschedule(all: List<Reminder>, now: Long): List<Reminder> = Remind.left(all, now)

    /** One alarm per reminder. */
    fun requestCode(id: String) = id.hashCode()
}
