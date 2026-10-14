package com.smizflark.aigile.messenger.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import coil.Coil
import coil.ImageLoader
import coil.intercept.Interceptor
import coil.request.ErrorResult
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.ui.Appearance
import com.smizflark.aigile.messenger.ui.MessageBubble
import com.smizflark.aigile.messenger.ui.MessengerTheme
import com.smizflark.aigile.messenger.ui.Row
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-188: a GIF link shows the GIF in the bubble; a link that fails to load just hides. The web is faked
 * (no picture in the shot: Coil does not draw under Robolectric). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class GifBubbleTest : ShotBase() {
    private fun fakeWeb(): ImageLoader {
        val ctx = RuntimeEnvironment.getApplication()
        val bmp = Bitmap.createBitmap(480, 270, Bitmap.Config.ARGB_8888).also {
            val c = Canvas(it); c.drawColor(0xFF7E57C2.toInt())
            c.drawText("GIF", 170f, 170f, Paint().apply { color = 0xFFFFFFFF.toInt(); textSize = 96f; isAntiAlias = true })
        }
        val file = java.io.File(ctx.cacheDir, "fake.png").also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        return ImageLoader.Builder(ctx).components {
            add(Interceptor { chain ->
                val url = chain.request.data.toString()
                if ("broken" in url) ErrorResult(null, chain.request, IllegalStateException("404"))
                else chain.proceed(chain.request.newBuilder().data(file).build())
            })
        }.build()
    }

    @After fun reset() { Coil.reset(); Appearance.gifs = true }

    private fun show(text: String) {
        Coil.setImageLoader(fakeWeb())
        val msg = Bubble("gif", "Lead", text, "2026-10-14T09:00:00Z", mine = false)
        rule.setContent {
            MessengerTheme {
                MessageBubble(Row.Msg(msg.key, msg), lead(), repo, 0, onReply = {}, onForward = {}, cards = emptySet(), onCard = {},
                    onOption = {}, onRetry = {}, onDiscard = {})
            }
        }
        rule.waitForIdle()
    }

    @Test fun gifPlaysInTheBubble() {
        RuntimeEnvironment.setQualifiers("+notnight")
        show("- Shipped it https://giphy.com/gifs/party-l0MYt5jPR6QX5pnqM")
        rule.onAllNodesWithTag("pic").assertCountEquals(1)
    }

    /** AII-198: with Play GIF links off, the link stays a link and nothing loads. */
    @Test fun switchedOff() {
        Appearance.gifs = false
        show("- Shipped it https://giphy.com/gifs/party-l0MYt5jPR6QX5pnqM")
        rule.onAllNodesWithTag("pic").assertCountEquals(0)
    }

    @Test fun brokenGifHides() {
        show("- See https://example.com/broken.gif")
        rule.onAllNodesWithTag("pic").assertCountEquals(0)
    }
}
