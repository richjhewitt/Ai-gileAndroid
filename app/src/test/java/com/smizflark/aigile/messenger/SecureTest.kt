package com.smizflark.aigile.messenger

import android.view.WindowManager
import com.smizflark.aigile.messenger.data.Secure
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** AII-402: Block screenshots sets FLAG_SECURE on the window; off clears it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureTest {
    @Test fun flagSecureFollowsTheSwitch() {
        val window = Robolectric.buildActivity(MainActivity::class.java).get().window
        Secure.apply(window, true)
        assertEquals(WindowManager.LayoutParams.FLAG_SECURE, window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE)
        Secure.apply(window, false)
        assertEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE)
    }
}
