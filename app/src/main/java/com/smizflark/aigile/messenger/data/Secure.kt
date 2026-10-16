package com.smizflark.aigile.messenger.data

import android.view.Window
import android.view.WindowManager

/** AII-402: Block screenshots; FLAG_SECURE hides the window from screenshots, screen recording and the recents preview. */
object Secure {
    fun apply(window: Window, on: Boolean) {
        if (on) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
