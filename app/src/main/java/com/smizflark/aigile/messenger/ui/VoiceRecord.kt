package com.smizflark.aigile.messenger.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Voice
import com.smizflark.aigile.messenger.data.VoiceLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/* ---------- recording a voice note (AII-331) ---------- */

/** Hold the mic to record, let go to send, slide away to cancel. [onNote] gets the finished file's path. */
class VoiceController internal constructor(
    private val ctx: Context, private val scope: CoroutineScope,
    private val onNote: (String) -> Unit, private val askPermission: () -> Unit,
) {
    var recording by mutableStateOf(false); private set
    var elapsed by mutableLongStateOf(0L); private set
    /** The finger has slid off the button: letting go now drops the note. */
    var cancelling by mutableStateOf(false)
    /** AII-384: slid up past the lock; letting go no longer ends the note. */
    var state by mutableStateOf(VoiceLock.State.HOLD); private set
    val locked get() = recording && state == VoiceLock.State.LOCKED
    fun slide(upDp: Float) { state = VoiceLock.onMove(state, upDp) }
    /** The finger came up: ends the note unless it is locked. */
    fun release() { if (VoiceLock.onRelease(state, cancelling) != VoiceLock.State.LOCKED) end(cancel = cancelling) }
    fun sendLocked() { if (locked) end(cancel = false) }
    fun deleteLocked() { if (locked) end(cancel = true) }
    private var rec: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L
    private var ticker: Job? = null

    /** The hold began. The first time, this only asks for the microphone. */
    fun begin() {
        if (recording) return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { askPermission(); return }
        val dir = File(ctx.cacheDir, "voice").apply { mkdirs() }
        val out = File(dir, Voice.recordingFile(System.currentTimeMillis()))
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44_100); r.setAudioEncodingBitRate(64_000)
            r.setOutputFile(out.path)
            r.prepare(); r.start()
        } catch (e: Exception) {
            runCatching { r.release() }; out.delete()
            Toast.makeText(ctx, R.string.voice_failed, Toast.LENGTH_SHORT).show(); return
        }
        rec = r; file = out; startedAt = System.currentTimeMillis(); elapsed = 0; cancelling = false; state = VoiceLock.State.HOLD; recording = true
        ticker = scope.launch {
            while (recording) {
                delay(100)
                elapsed = System.currentTimeMillis() - startedAt
                if (elapsed >= Voice.MAX_MS) end(cancel = false)   // 2 minutes: it sends by itself
            }
        }
    }

    /** The finger came up (or the chat closed). A note under a second, or a cancelled one, is deleted. */
    fun end(cancel: Boolean) {
        val r = rec ?: return
        val out = file
        val ms = System.currentTimeMillis() - startedAt
        rec = null; file = null; recording = false; state = VoiceLock.State.HOLD; ticker?.cancel()
        val stopped = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        if (cancel || !stopped || Voice.tooShort(ms) || out == null) {
            out?.delete()
            if (!cancel && Voice.tooShort(ms)) Toast.makeText(ctx, R.string.voice_too_short, Toast.LENGTH_SHORT).show()
            return
        }
        onNote(out.path)
    }

    internal companion object {
        /** For screenshots: a recording line with no recorder behind it. */
        fun preview(ctx: Context, scope: CoroutineScope, ms: Long, locked: Boolean = false) = VoiceController(ctx, scope, {}, {}).apply { recording = true; elapsed = ms; if (locked) slide(VoiceLock.LOCK_DP) }
    }
}

@Composable
internal fun rememberVoiceController(onNote: (String) -> Unit): VoiceController {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(onNote)
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) Toast.makeText(ctx, R.string.voice_denied, Toast.LENGTH_LONG).show()
    }
    val c = remember { VoiceController(ctx, scope, { latest(it) }, { ask.launch(Manifest.permission.RECORD_AUDIO) }) }
    DisposableEffect(c) { onDispose { c.end(cancel = true) } }   // leaving the chat drops a note in progress
    return c
}

/**
 * A press on the mic: a tap runs [onTap] (voice to text); holding records until the finger comes up;
 * moving the finger well off the button marks the note as cancelled.
 */
internal fun Modifier.holdToRecord(voice: VoiceController, onTap: () -> Unit): Modifier = pointerInput(voice) {
    val margin = 40.dp.toPx()
    val lockPx = VoiceLock.LOCK_DP.dp.toPx()
    awaitEachGesture {
        awaitFirstDown()
        var released = false
        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation().also { released = true } }
        if (released) { if (up != null) onTap(); return@awaitEachGesture }
        voice.begin()
        if (!voice.recording) { waitForUpOrCancellation(); return@awaitEachGesture }   // the first hold only asked for the microphone
        while (true) {
            val c = awaitPointerEvent().changes.first()
            c.consume()
            if (!c.pressed) { voice.release(); return@awaitEachGesture }
            val p = c.position
            voice.slide(-p.y / lockPx * VoiceLock.LOCK_DP)   // up is negative y
            voice.cancelling = !voice.locked && (p.x < -margin || p.x > size.width + margin || p.y > size.height + margin)
        }
    }
}

/** "Recording… 0:07 · slide away to cancel" above the message box while the mic is held; locked: timer, Delete and Send. */
@Composable
internal fun RecordingLine(voice: VoiceController) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).testTag("recording"), verticalAlignment = Alignment.CenterVertically) {
        if (voice.locked) {
            IconButton(onClick = { voice.deleteLocked() }, modifier = Modifier.testTag("voice-delete")) {
                Icon(Icons.Default.Delete, stringResource(R.string.voice_delete), tint = cs.error)
            }
            Text("●", color = cs.error, modifier = Modifier.padding(end = 6.dp))
            Text(Voice.label(voice.elapsed), style = MaterialTheme.typography.bodyMedium, color = cs.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = { voice.sendLocked() }, modifier = Modifier.testTag("voice-send")) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.voice_send), tint = cs.primary)
            }
        } else {
            Text("●", color = cs.error, modifier = Modifier.padding(end = 6.dp))
            Text(stringResource(R.string.voice_recording, Voice.label(voice.elapsed)), style = MaterialTheme.typography.bodyMedium,
                color = if (voice.cancelling) cs.error else cs.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Lock, stringResource(R.string.voice_lock_hint), Modifier.testTag("voice-lock-hint"), tint = cs.onSurfaceVariant)
        }
    }
}
