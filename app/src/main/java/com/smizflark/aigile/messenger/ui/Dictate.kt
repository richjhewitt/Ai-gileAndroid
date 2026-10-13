package com.smizflark.aigile.messenger.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag

/** Voice to text: the phone's own speech recogniser, so no extra permission or app key. */
object Dictate {
    /** Adds dictated words after what's already typed, with one space between. */
    fun join(typed: String, words: String): String {
        val w = words.trim()
        if (w.isEmpty()) return typed
        if (typed.isBlank()) return w.replaceFirstChar { it.uppercase() }
        return if (typed.last().isWhitespace()) typed + w else "$typed $w"
    }
}

/** 🎤 button for a message box. Hands back what was said; nothing if the person cancels. */
@Composable
fun MicButton(modifier: Modifier = Modifier, onWords: (String) -> Unit) {
    val context = LocalContext.current
    val listen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onWords)
    }
    IconButton(
        onClick = {
            val ask = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your message")
            try { listen.launch(ask) }
            catch (e: ActivityNotFoundException) { Toast.makeText(context, "This phone has no voice typing.", Toast.LENGTH_SHORT).show() }
        },
        modifier = modifier.testTag("mic"),
    ) { Icon(Icons.Default.Mic, "Dictate a message") }
}
