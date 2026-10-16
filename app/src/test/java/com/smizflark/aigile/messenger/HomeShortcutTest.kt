package com.smizflark.aigile.messenger

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.HomeShortcut
import com.smizflark.aigile.messenger.work.CheckWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeShortcutTest {
    @Test fun idIsOnePerChat() {
        assertEquals("home:lead-1", HomeShortcut.id("lead-1"))
        assertNotEquals(HomeShortcut.id("a"), HomeShortcut.id("b"))
    }

    @Test fun intentOpensTheChatLikeAnAlert() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val i = HomeShortcut.intent(ctx, "lead-1")
        assertEquals(Intent.ACTION_VIEW, i.action)
        assertEquals("lead-1", i.getStringExtra(CheckWorker.EXTRA_CHAT))
        assertEquals(MainActivity::class.java.name, i.component?.className)
    }
}
