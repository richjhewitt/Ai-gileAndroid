package com.smizflark.aigile.messenger

import android.app.Application
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.work.CheckWorker

class App : Application() {
    val repo by lazy { Repo(this) }

    override fun onCreate() {
        super.onCreate()
        CheckWorker.channel(this)
        if (repo.store.connected && !repo.store.demo && repo.store.alerts) CheckWorker.schedule(this)
    }

    companion object {
        /** No alerts while you're looking at the app. */
        @Volatile var inForeground = false
    }
}
