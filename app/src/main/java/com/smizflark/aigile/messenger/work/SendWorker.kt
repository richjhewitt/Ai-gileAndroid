package com.smizflark.aigile.messenger.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smizflark.aigile.messenger.App
import com.smizflark.aigile.messenger.data.BoardError
import com.smizflark.aigile.messenger.data.Outbox
import com.smizflark.aigile.messenger.data.Pending
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Sends messages typed with no signal once the phone is back online, even with the app closed (AII-55).
 * Android runs it when there is a network; if the board still can't be reached it tries again later.
 */
class SendWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as App).repo
        if (!repo.store.connected || repo.store.demo) return Result.success()
        val giveUp = runAttemptCount >= MAX_TRIES
        val before = repo.store.outbox()
        val left = repo.sendWaiting(giveUp)
        // AII-151: one that gave up while you weren't looking gets an alert.
        val names = repo.store.cachedChats().orEmpty().associate { it.key to it.name }
        Outbox.newlyFailed(before, repo.store.outbox()).forEach { p ->
            ChatNotifier.notSent(applicationContext, p.chat, names[p.chat] ?: p.chat, p.id, p.text)
        }
        return if (left > 0 && !giveUp) Result.retry() else Result.success()
    }

    companion object {
        private const val NAME = "send-waiting"
        /** About a day of tries with backoff; after that the message shows Not sent, with Tap to retry. */
        const val MAX_TRIES = 12

        fun schedule(context: Context) {
            val req = OneTimeWorkRequestBuilder<SendWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.KEEP, req)
        }

        /** Send later (AII-146): wakes at [at] to send what is due, even with the app closed. */
        fun scheduleAt(context: Context, id: String, at: Instant) {
            val req = OneTimeWorkRequestBuilder<SendWorker>()
                .setInitialDelay(Duration.between(Instant.now(), at).toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("$NAME-$id", ExistingWorkPolicy.REPLACE, req)
        }
    }
}

/** Send later (AII-146): still held back at [now]. */
fun later(p: Pending, now: Instant = Instant.now()) = p.sendAt?.let { runCatching { Instant.parse(it) > now }.getOrNull() } == true

/** The board couldn't be reached at all (no signal, PC off), as opposed to the board saying no. */
fun isOffline(e: Throwable) = e is BoardError && e.code == 0

/** Messages that should still go by themselves: never tried, or stopped only by having no connection; Send later ones once due. */
fun waitingToSend(outbox: List<Pending>, now: Instant = Instant.now()) = outbox.filter { (!it.failed || it.offline) && !later(it, now) }
