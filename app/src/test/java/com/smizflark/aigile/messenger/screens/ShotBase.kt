package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.ui.Appearance
import com.smizflark.aigile.messenger.ui.Look
import com.smizflark.aigile.messenger.ui.MessengerTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.robolectric.RuntimeEnvironment

/** AII-20: what every screenshot test shares: the demo data, a fixed clock, and the shot helpers. */
abstract class ShotBase {
    @get:Rule val rule = createComposeRule()
    protected lateinit var repo: Repo
    protected lateinit var chats: List<ChatRef>

    @Before fun setUp() {
        // AII-82: same time and place on every run, so a shot only changes when the screen does.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/London"))
        Now.clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-13T13:00:00Z"), java.time.ZoneId.of("Europe/London"))
        repo = Repo(ApplicationProvider.getApplicationContext<Application>())
        repo.startDemo()
        chats = runBlocking { repo.chats() }
        repo.store.updateChecked = Now.millis()   // AII-132: no GitHub call in screenshot tests
        // AII-179: Settings loads the saved settings (quick replies on) afresh each test; every other shot keeps them off as before.
        Appearance.look = null; Appearance.quickReplies = false; Appearance.replies = emptyList()
    }

    @After fun tearDown() { Now.clock = java.time.Clock.systemDefaultZone() }

    protected fun shot(name: String, dark: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        show(dark, content)
        rule.onRoot().captureRoboImage("../docs/shots/${name}_${if (dark) "dark" else "light"}.png")
    }

    /** Set the screen without a shot, for tests that type or tap before capturing. */
    protected fun show(dark: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers(if (dark) "+night" else "+notnight")
        rule.setContent { MessengerTheme { content() } }
        rule.waitForIdle()
    }

    protected fun lead() = chats.first { it.id == "ai-gile" }
    protected fun prime(ref: ChatRef) { runBlocking { repo.conversation(ref) } }

    /** AII-69: the pure black (OLED) look, so every screen can be checked for grey panels. */
    protected fun black(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        Appearance.look = Look.Black; Appearance.phoneColours = false
        try {
            rule.setContent { MessengerTheme { content() } }
            rule.waitForIdle()
            rule.onRoot().captureRoboImage("../docs/shots/${name}_black.png")
        } finally { Appearance.look = null; Appearance.phoneColours = true }
    }
}
