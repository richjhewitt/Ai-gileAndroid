package com.smizflark.aigile.messenger.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.CardDetail
import com.smizflark.aigile.messenger.data.ChatSound
import com.smizflark.aigile.messenger.data.ChatInfo
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Star
import androidx.compose.ui.text.style.TextOverflow
import com.smizflark.aigile.messenger.data.togglePin
import kotlinx.coroutines.launch

/** AII-229: writes [text] to Downloads (MediaStore from Android 10, else the app's own external files folder); true when it worked. */
internal fun saveTextFile(ctx: android.content.Context, name: String, text: String): Boolean = runCatching {
    if (android.os.Build.VERSION.SDK_INT >= 29) {
        val v = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name)
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
        }
        val res = ctx.contentResolver
        val uri = res.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: error("no uri")
        res.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
        res.update(uri, android.content.ContentValues().apply { put(android.provider.MediaStore.Downloads.IS_PENDING, 0) }, null, null)
    } else {
        val dir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: error("no dir")
        dir.mkdirs(); java.io.File(dir, name).writeText(text)
    }
}.isSuccess

/** Tap the chat name (AII-80): the team, its open cards, the pictures sent here, and the Mute and Pin switches. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatDetails(ref: ChatRef, repo: Repo, info: ChatInfo, onFind: (() -> Unit)? = null, starred: List<Star> = emptyList(), onStarred: ((String) -> Unit)? = null, onShare: (() -> Unit)? = null, onSave: (() -> Unit)? = null, onReminder: ((String) -> Unit)? = null, onChecked: (() -> Unit)? = null, onClear: (() -> Unit)? = null, onClose: () -> Unit) {
    var checked by remember { mutableStateOf(ref.checked) }   // AII-324: safety code checked together
    var muted by remember { mutableStateOf(ref.key in repo.store.muted()) }
    var silent by remember { mutableStateOf(ref.key in repo.store.silent) }
    var pinned by remember { mutableStateOf(ref.key in repo.store.pinned) }
    var muteAsk by remember { mutableStateOf(false) }   // AII-157: Mute asks how long
    var muteEnds by remember { mutableStateOf(repo.store.mutedUntil()[ref.key]) }   // AII-160: when a timed mute ends
    if (muteAsk) MuteFor(onPick = { f -> repo.store.setMuted(ref.key, true, com.smizflark.aigile.messenger.data.Mute.until(com.smizflark.aigile.messenger.data.Now.millis(), f)); muted = true; muteAsk = false; muteEnds = repo.store.mutedUntil()[ref.key] },
        onDismiss = { muteAsk = false })
    var shown by remember { mutableStateOf<CardDetail?>(null) }   // AII-85: the open card tapped, shown under its row
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var mine by remember { mutableStateOf(com.smizflark.aigile.messenger.data.Remind.inChat(repo.store.reminders(), ref.key)) }   // AII-223
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().testTag("chat-details")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(ref, repo, 56.dp)
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(ref.name, style = MaterialTheme.typography.titleLarge)
                    Text(ref.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // AII-203: what the app has for this chat.
            info.about?.let { ab ->
                val n = androidx.compose.ui.res.pluralStringResource(R.plurals.about_chat_count, ab.total, ab.total)
                Text(if (ab.since != null) stringResource(R.string.about_chat, n, com.smizflark.aigile.messenger.data.aboutDate(ab.since), ab.mine) else stringResource(R.string.about_chat_nodate, n, ab.mine),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp).testTag("details-about"))
            }
            // AII-324: the six words both phones show; read them to each other.
            if (ref.kind == com.smizflark.aigile.messenger.data.Kind.FRIEND && ref.safety.isNotBlank()) {
                Text(stringResource(R.string.safety_code), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                Text(ref.safety, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth().testTag("safety-words"))
                Text(stringResource(R.string.safety_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                if (checked) Text(stringResource(R.string.safety_checked), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp).testTag("safety-checked"))
                else if (onChecked != null) androidx.compose.material3.Button(onClick = { checked = true; onChecked() },
                    modifier = Modifier.padding(top = 8.dp).testTag("we-checked-it")) { Text(stringResource(R.string.we_checked_it)) }
            }
            // AII-128: straight to the find bar from here.
            FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onFind != null) OutlinedButton(onClick = onFind, modifier = Modifier.testTag("details-find")) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.find_in_chat))
                }
                // AII-139: the whole chat as text, out to any app.
                if (onShare != null) OutlinedButton(onClick = onShare, modifier = Modifier.testTag("details-share")) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.share_chat))
                }
                // AII-229: the same text as a .txt in Downloads.
                if (onSave != null) OutlinedButton(onClick = onSave, modifier = Modifier.testTag("details-save")) {
                    Icon(Icons.Default.Done, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.save_chat))
                }
                // AII-363: hide the messages so far, this phone only.
                if (onClear != null) OutlinedButton(onClick = onClear, modifier = Modifier.testTag("details-clear")) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.clear_chat))
                }
            }
            // AII-245: Mute and Silent alerts side by side.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SwitchRow(stringResource(R.string.mute_alerts), muted, "details-mute", Modifier.weight(1f)) {
                    if (it) muteAsk = true else { muted = false; muteEnds = null; repo.store.setMuted(ref.key, false) }
                }
                SwitchRow(stringResource(R.string.silent_alerts), silent, "details-silent", Modifier.weight(1f)) {
                    repo.store.silent = com.smizflark.aigile.messenger.data.Silent.toggle(repo.store.silent, ref.key); silent = ref.key in repo.store.silent
                }
            }
            // AII-373: this chat's own alert sound, picked in the phone's ringtone picker.
            val ctx = androidx.compose.ui.platform.LocalContext.current
            var sound by remember { mutableStateOf(ChatSound.of(repo.store.chatSounds, ref.key)) }
            val pickSound = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
                if (r.resultCode != android.app.Activity.RESULT_OK) return@rememberLauncherForActivityResult
                val picked = r.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                val default = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                sound = when (picked) { null -> ChatSound.NONE; default -> ""; else -> picked.toString() }
                repo.store.chatSounds = ChatSound.put(repo.store.chatSounds, ref.key, sound)
                com.smizflark.aigile.messenger.work.ChatNotifier.channelFor(ctx, ref.key, ref.name)   // makes the new channel, deletes the old one
            }
            val soundName = when {
                sound.isEmpty() -> stringResource(R.string.alert_sound_default)
                ChatSound.isNone(sound) -> stringResource(R.string.alert_sound_none)
                else -> remember(sound) { runCatching { android.media.RingtoneManager.getRingtone(ctx, android.net.Uri.parse(sound)).getTitle(ctx) }.getOrNull() } ?: stringResource(R.string.alert_sound_default)
            }
            Row(Modifier.fillMaxWidth().clickable {
                pickSound.launch(android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, when {
                        sound.isEmpty() -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        ChatSound.isNone(sound) -> null
                        else -> android.net.Uri.parse(sound)
                    })
                })
            }.padding(top = 12.dp, bottom = 4.dp).testTag("details-sound"), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.alert_sound), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
                Text(soundName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            muteEnds?.takeIf { muted }?.let { end ->
                Text(stringResource(R.string.muted_until, com.smizflark.aigile.messenger.data.Mute.endTime(end)), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("details-mute-ends"))
            }
            SwitchRow(stringResource(R.string.pin), pinned, "details-pin") {
                repo.store.pinned = togglePin(repo.store.pinned, ref.key); pinned = ref.key in repo.store.pinned
            }
            Text(heading(R.string.open_cards, info.openCards.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            if (info.openCards.isEmpty()) Text(stringResource(R.string.no_open_cards), color = MaterialTheme.colorScheme.onSurfaceVariant)
            info.openCards.forEach { t ->
                Text("⏳ ${t.label}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth()
                    .clickable { if (shown?.id == t.id) shown = null else scope.launch { shown = t.id?.let { repo.card(ref, it) } } }
                    .padding(vertical = 6.dp).testTag("details-card-${t.id}"))
                shown?.takeIf { it.id == t.id }?.let { CardDetails(it, ref, repo) { d -> shown = d } }
            }
            // AII-138: starred messages; tap one and the chat jumps to it.
            if (onStarred != null) {
                Text(heading(R.string.starred, starred.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                if (starred.isEmpty()) Text(stringResource(R.string.no_starred), color = MaterialTheme.colorScheme.onSurfaceVariant)
                starred.forEach { s ->
                    Column(Modifier.fillMaxWidth().clickable { onStarred(s.key) }.padding(vertical = 6.dp).testTag("details-star")) {
                        val plain = Markdown.render(s.text, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary).text   // without the ** marks
                        Text("★ " + com.smizflark.aigile.messenger.data.Stars.preview(plain),style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(listOf(s.author, fullTime(s.at)).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // AII-223: this chat's reminders, soonest first; tap one to jump to its message, X to cancel.
            if (mine.isNotEmpty() && onReminder != null) {
                Text(stringResource(R.string.reminders), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                mine.forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.go_to_message)) { onReminder(r.msg) }.padding(start = 0.dp).testTag("details-reminder"), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                            Text(r.text.replace('\n', ' '), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("⏰ " + sendTime(java.time.Instant.ofEpochMilli(r.at).toString()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        androidx.compose.material3.IconButton(onClick = {
                            cancelReminder(ctx, repo, r.id); mine = com.smizflark.aigile.messenger.data.Remind.inChat(repo.store.reminders(), ref.key)
                        }, modifier = Modifier.testTag("details-reminder-x")) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Close, stringResource(R.string.cancel_reminder))
                        }
                    }
                }
            }
            Text(heading(R.string.chat_pictures, info.pictures.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            if (info.pictures.isEmpty()) Text(stringResource(R.string.no_chat_pictures), color = MaterialTheme.colorScheme.onSurfaceVariant)
            // The sheet is its own window and would cover the full screen picture, so it closes first (AII-93).
            else MessagePictures(info.pictures, repo, onOpened = onClose)
            // AII-194: web links from the messages, newest first.
            if (info.links.isNotEmpty()) {
                Text(heading(R.string.chat_links, info.links.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                info.links.forEach { l ->
                    Column(Modifier.fillMaxWidth().clickable { runCatching { uriHandler.openUri(l.url) } }.padding(vertical = 6.dp).testTag("details-link")) {
                        Text(l.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val address = com.smizflark.aigile.messenger.data.ChatLink.shown(l.url)
                        if (address != l.title) Text(address, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            // AII-197: files that are not pictures; tap to open.
            if (info.files.isNotEmpty()) {
                Text(heading(R.string.chat_files, info.files.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                info.files.forEach { f -> FileLine(f, repo, Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("details-file")) }
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(label: String, on: Boolean, tag: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (Boolean) -> Unit) {
    Row(modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
        Switch(checked = on, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

/** AII-252: a section heading with how many it lists; no number while the section is empty. */
@Composable
private fun heading(res: Int, count: Int): String =
    if (count > 0) stringResource(R.string.heading_count, stringResource(res), count) else stringResource(res)
