package com.smizflark.aigile.messenger.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Voice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/* ---------- playing voice notes (AII-331) ---------- */

/** One note plays at a time, for the whole app; leaving the chat stops it. */
object VoicePlayer {
    /** The note that is loaded (its Pic path), and whether it is playing now. */
    var key by mutableStateOf<String?>(null); private set
    var playing by mutableStateOf(false); private set
    var position by mutableIntStateOf(0); private set
    /** Lengths found so far, by Pic path. */
    internal val lengths = mutableStateMapOf<String, Long>()
    /** Playback speed (AII-378); the Store keeps it, VoiceNote loads it. */
    var speed by mutableStateOf(1f); private set
    private var mp: MediaPlayer? = null

    fun chooseSpeed(s: Float) { speed = s; applySpeed() }

    /** Only while playing: setting params on a paused player can start it. */
    private fun applySpeed() {
        val m = mp ?: return
        if (playing) runCatching { m.playbackParams = m.playbackParams.setSpeed(speed) }
    }

    fun stop() {
        mp?.let { runCatching { it.stop() }; runCatching { it.release() } }
        mp = null; key = null; playing = false; position = 0
    }

    /** Play, pause or resume [path] from [file]; false when the file can't be played. */
    fun toggle(path: String, file: File): Boolean {
        val cur = mp
        if (cur != null && key == path) {
            if (playing) { cur.pause(); playing = false } else { cur.start(); playing = true; applySpeed() }
            return true
        }
        stop()
        return try {
            val p = MediaPlayer()
            p.setDataSource(file.path); p.prepare()
            p.setOnCompletionListener { stop() }
            p.start()
            mp = p; key = path; playing = true; applySpeed(); lengths[path] = p.duration.toLong()
            true
        } catch (e: Exception) { stop(); false }
    }

    internal fun sync() { mp?.let { position = runCatching { it.currentPosition }.getOrDefault(0) } }
}

/** The note on disk: a file on the phone as it is, a board file fetched (with this phone's key) into the cache. */
private fun resolve(ctx: Context, p: Pic, repo: Repo): File? {
    if (p.path.startsWith("file:")) Uri.parse(p.path).path?.let { f -> File(f).takeIf { it.exists() }?.let { return it } }
    val dir = File(ctx.cacheDir, "voice").apply { mkdirs() }
    val name = "${p.path.hashCode()}-"
    File(dir, name + p.name.ifEmpty { "note.m4a" }).takeIf { it.exists() && it.length() > 0 }?.let { return it }
    return fetch(ctx, p, repo, dir, name)
}

private fun lengthOf(file: File): Long? = runCatching {
    val r = MediaMetadataRetriever()
    try { r.setDataSource(file.path); r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() } finally { r.release() }
}.getOrNull()

/** A voice note in a bubble: play / pause, a thin progress bar and the length. */
@Composable
fun VoiceNote(p: Pic, repo: Repo) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val mine = VoicePlayer.key == p.path
    val playing = mine && VoicePlayer.playing
    val known = VoicePlayer.lengths[p.path]
    LaunchedEffect(p.path) {   // the length shows before it is played (small file, kept in the cache)
        if (VoicePlayer.lengths[p.path] == null) withContext(Dispatchers.IO) {
            runCatching { resolve(ctx, p, repo)?.let(::lengthOf) }.getOrNull()?.let { VoicePlayer.lengths[p.path] = it }
        }
    }
    LaunchedEffect(playing) { while (playing) { VoicePlayer.sync(); delay(150) } }
    LaunchedEffect(Unit) { VoicePlayer.chooseSpeed(repo.store.voiceSpeed) }
    val speed = VoicePlayer.speed
    val speedLabel = Voice.speedLabel(speed)
    val label = if (known != null) Voice.label(known) else "–:–"
    val cs = MaterialTheme.colorScheme
    val desc = if (playing) stringResource(R.string.voice_pause) else if (known != null) stringResource(R.string.voice_play_len, label) else stringResource(R.string.voice_play)
    val speedDesc = stringResource(R.string.voice_speed, speedLabel)
    val cant = stringResource(R.string.voice_cant_play)
    Row(Modifier.widthIn(min = 200.dp, max = 260.dp).fillMaxWidth().padding(bottom = 4.dp).testTag("voice-note"), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            scope.launch {
                val f = withContext(Dispatchers.IO) { runCatching { resolve(ctx, p, repo) }.getOrNull() }
                if (f == null || !VoicePlayer.toggle(p.path, f)) Toast.makeText(ctx, cant, Toast.LENGTH_SHORT).show()
            }
        }, modifier = Modifier.size(40.dp).semantics { contentDescription = desc }.testTag("voice-play")) {
            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = cs.primary)
        }
        val frac = if (mine && known != null && known > 0) (VoicePlayer.position.toFloat() / known).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(progress = { frac }, modifier = Modifier.weight(1f).height(3.dp).testTag("voice-progress"))
        Text(
            speedLabel, style = MaterialTheme.typography.labelMedium, color = cs.primary,
            modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(50)).border(1.dp, cs.primary, RoundedCornerShape(50))
                .clickable { val n = Voice.nextSpeed(speed); repo.store.voiceSpeed = n; VoicePlayer.chooseSpeed(n) }
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .semantics { contentDescription = speedDesc }.testTag("voice-speed"),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
    }
}
