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
import com.smizflark.aigile.messenger.data.Pending
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
        val left = repo.sendWaiting(giveUp)
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
    }
}

/** The board couldn't be reached at all (no signal, PC off), as opposed to the board saying no. */
fun isOffline(e: Throwable) = e is BoardError && e.code == 0

/** Messages that should still go by themselves: never tried, or stopped only by having no connection. */
fun waitingToSend(outbox: List<Pending>) = outbox.filter { !it.failed || it.offline }
