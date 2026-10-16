package com.smizflark.aigile.messenger.screens

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.ui.ChatScreen
import com.smizflark.aigile.messenger.ui.MessengerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** AII-399: the system back gesture from a chat returns to the chats list (works the same with predictive back on). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class BackToChatsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun backFromChatShowsList() {
        val repo = Repo(ApplicationProvider.getApplicationContext<Application>())
        repo.startDemo()
        val ref = runBlocking { repo.chats() }.first { it.id == "ai-gile" }
        runBlocking { repo.conversation(ref) }
        rule.setContent {
            MessengerTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "chats") {
                    composable("chats") { Text("the chats list") }
                    composable("chat") { ChatScreen(repo, ref, onBack = { nav.popBackStack() }) }
                }
                LaunchedEffect(Unit) { nav.navigate("chat") }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("the chats list").assertDoesNotExist()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithText("the chats list").assertExists()
    }
}
