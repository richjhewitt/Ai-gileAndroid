package com.smizflark.aigile.messenger

import android.app.Application
import android.content.res.Configuration
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.smizflark.aigile.messenger.data.Crash
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.ui.Words
import com.smizflark.aigile.messenger.work.CheckWorker

class App : Application(), ImageLoaderFactory {
    val repo by lazy { Repo(this) }

    override fun onCreate() {
        super.onCreate()
        Words.load(this)
        Crash.install(this)
        CheckWorker.channel(this)
        if (repo.store.connected && !repo.store.demo && repo.store.alerts) CheckWorker.schedule(this)
    }

    /** Pictures as before, and GIFs that move (AII-188). */
    override fun newImageLoader() = ImageLoader.Builder(this).components {
        add(if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory())
    }.build()

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
