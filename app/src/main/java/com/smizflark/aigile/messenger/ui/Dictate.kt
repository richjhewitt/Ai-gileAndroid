package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R

import androidx.compose.ui.res.stringResource

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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

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
fun MicButton(modifier: Modifier = Modifier, voice: VoiceController? = null, onWords: (String) -> Unit) {
    val context = LocalContext.current
    val prompt = stringResource(R.string.dictate_prompt)
    val noVoice = stringResource(R.string.no_voice)
    val listen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onWords)
    }
    val dictate = {
        val ask = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        try { listen.launch(ask) }
        catch (e: ActivityNotFoundException) { Toast.makeText(context, noVoice, Toast.LENGTH_SHORT).show() }
    }
    if (voice != null) {   // AII-331: a friend chat: tap = voice to text, hold = record a voice note
        val holdLabel = stringResource(R.string.voice_hold_label)
        androidx.compose.foundation.layout.Box(
            modifier.size(48.dp).holdToRecord(voice, dictate).semantics { role = Role.Button; onClick(label = null) { dictate(); true }; onLongClick(label = holdLabel) { false } }.testTag("mic"),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) { Icon(Icons.Default.Mic, stringResource(R.string.dictate)) }
        return
    }
    IconButton(onClick = dictate, modifier = modifier.testTag("mic")) { Icon(Icons.Default.Mic, stringResource(R.string.dictate)) }
}
