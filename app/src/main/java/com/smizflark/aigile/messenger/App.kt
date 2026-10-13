package com.smizflark.aigile.messenger

import android.app.Application
import android.content.res.Configuration
import com.smizflark.aigile.messenger.data.Crash
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.ui.Words
import com.smizflark.aigile.messenger.work.CheckWorker

class App : Application() {
    val repo by lazy { Repo(this) }

    override fun onCreate() {
        super.onCreate()
        Words.load(this)
        Crash.install(this)
        CheckWorker.channel(this)
        if (repo.store.connected && !repo.store.demo && repo.store.alerts) CheckWorker.schedule(this)
    }

    // The phone's language changed while the app was running.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Words.load(this)
    }

    companion object {
        /** No alerts while you're looking at the app. */
        @Volatile var inForeground = false
    }
}
