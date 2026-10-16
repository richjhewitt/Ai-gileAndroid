package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R

import androidx.compose.ui.res.stringResource

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import com.smizflark.aigile.messenger.data.UndoSend
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.BuildConfig
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Crash
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.Replies
import androidx.compose.ui.res.stringArrayResource
import com.smizflark.aigile.messenger.work.CheckWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import coil.imageLoader
import com.smizflark.aigile.messenger.data.SavedPictures

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(repo: Repo, onBack: () -> Unit, onSignedOut: (wasDemo: Boolean) -> Unit, onReminders: () -> Unit = {}, onAbout: () -> Unit = {}, showReplies: Boolean = false) {
    val s = repo.store
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf(s.alerts) }
    var lockOn by remember { mutableStateOf(s.lock) }
    var lockAfter by remember { mutableStateOf(s.lockAfter) }   // AII-234
    var quietOn by remember { mutableStateOf(s.quietOn) }
    var chatHeads by remember { mutableStateOf(s.chatHeads) }   // AII-381
    var sentAlerts by remember { mutableStateOf(s.sentAlerts) }   // AII-175
    var from by remember { mutableStateOf(s.quietFrom) }
    var to by remember { mutableStateOf(s.quietTo) }
    var morning by remember { mutableStateOf(s.morning) }   // AII-208
    var note by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableIntStateOf(0) }   // bumps after a change, so the connection text re-reads the store
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }          // AII-217
    var backupPass by remember { mutableStateOf<Boolean?>(null) }   // AII-349: true = export, false = restore; null = no dialog
    var passText by remember { mutableStateOf("") }
    var backupUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { if (it != null) { backupUri = it; backupPass = true } }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) { backupUri = it; backupPass = false } }
    var confirmClear by remember { mutableStateOf(false) }          // AII-215
    var savedBytes by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { savedBytes = withContext(Dispatchers.IO) { SavedPictures.size(ctx.cacheDir) } }
    var crash by remember { mutableStateOf(Crash.last(ctx)) }
    var bug by remember { mutableStateOf("") }
    var bugNote by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) note = ctx.getString(R.string.alerts_permission)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.settings)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(stringResource(R.string.connection), first = true)
            key(saved) {
                Text(if (s.demo) stringResource(R.string.demo_note)
                    else stringResource(R.string.board_info, s.url, s.owner, Mapper.maskKey(s.key)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("connection"))
            }
            OutlinedButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth().testTag("signOut")) {   // AII-285: right under the connection, easy to see
                Text(stringResource(if (s.demo) R.string.leave_demo else R.string.sign_out), color = MaterialTheme.colorScheme.error)
            }
            ChangeBoard(repo, onSaved = { owner ->
                saved++; note = ctx.getString(R.string.saved_as, owner)
                if (s.alerts) CheckWorker.schedule(ctx)
            })

            SectionHeader(stringResource(R.string.look))
            AppearanceSettings(s)
            Toggle(stringResource(R.string.show_times), stringResource(R.string.show_times_sub), Appearance.showTimes, Modifier.testTag("showTimes")) {
                Appearance.showTimes = it; s.showTimes = it
            }
            Toggle(stringResource(R.string.play_gifs), stringResource(R.string.play_gifs_sub), Appearance.gifs, Modifier.testTag("gifs")) {
                Appearance.gifs = it; s.gifs = it
            }
            LoadPicturesPick(Appearance.loadPictures) { Appearance.loadPictures = it; s.loadPictures = it }
            Toggle(stringResource(R.string.compact_list), stringResource(R.string.compact_list_sub), Appearance.compact, Modifier.testTag("compact")) {
                Appearance.compact = it; s.compact = it
            }
            Toggle(stringResource(R.string.muted_last), stringResource(R.string.muted_last_sub), Appearance.mutedLast, Modifier.testTag("mutedLast")) {
                Appearance.mutedLast = it; s.mutedLast = it
            }
            SortPick(Appearance.sortAz) { Appearance.sortAz = it; s.sortChats = if (it) "az" else "newest" }
            SwipePick(stringResource(R.string.swipe_right), Appearance.swipeRight, "swipeRight") { Appearance.swipeRight = it; s.swipeRight = it.id }
            SwipePick(stringResource(R.string.swipe_left), Appearance.swipeLeft, "swipeLeft") { Appearance.swipeLeft = it; s.swipeLeft = it.id }

            SectionHeader(stringResource(R.string.chats))
            Toggle(stringResource(R.string.enter_sends), stringResource(R.string.enter_sends_sub), Appearance.enterSends, Modifier.testTag("enterSends")) {
                Appearance.enterSends = it; s.enterSends = it
            }
            Toggle(stringResource(R.string.next_unread_setting), stringResource(R.string.next_unread_setting_sub), Appearance.nextUnread, Modifier.testTag("nextUnread")) {
                Appearance.nextUnread = it; s.nextUnread = it
            }
            // AII-209: only when there are any.
            val reminderCount = remember { s.reminders().size }
            if (reminderCount > 0) {
                Row(Modifier.fillMaxWidth().clickable(onClick = onReminders).padding(vertical = 8.dp).testTag("remindersRow"), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.reminders), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(reminderCount.toString(), color = MaterialTheme.colorScheme.primary)
                }
            }
            Toggle(stringResource(R.string.undo_send), stringResource(R.string.undo_send_sub), Appearance.undoSend, Modifier.testTag("undoSend")) {
                Appearance.undoSend = it; s.undoSend = it
            }
            // AII-233: how long it waits.
            if (Appearance.undoSend) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.undo_seconds), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    UndoSend.CHOICES.forEach { n ->
                        FilterChip(selected = UndoSend.seconds(Appearance.undoSeconds) == n, onClick = { Appearance.undoSeconds = n; s.undoSeconds = n },
                            label = { Text(stringResource(R.string.seconds_short, n)) }, modifier = Modifier.testTag("undoSeconds$n"))
                    }
                }
            }
            Toggle(stringResource(R.string.quick_replies), stringResource(R.string.quick_replies_sub), Appearance.quickReplies, Modifier.testTag("quickReplies")) {
                Appearance.quickReplies = it; s.quickReplies = it
            }
            // AII-180: write your own, one per line; clear the box to get the starting ones back.
            if (Appearance.quickReplies) {
                val starting = stringArrayResource(R.array.quick_replies_list).joinToString("\n")
                var mine by remember { mutableStateOf(s.quickReplyText.ifBlank { starting }) }
                // AII-255: coming from a held quick reply, bring the box into view.
                val bring = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
                if (showReplies) LaunchedEffect(Unit) { bring.bringIntoView() }
                OutlinedTextField(mine, { mine = it; s.quickReplyText = it; Appearance.replies = Replies.parse(it) },
                    Modifier.fillMaxWidth().bringIntoViewRequester(bring).testTag("quickReplyText"), label = { Text(stringResource(R.string.quick_replies_mine)) }, minLines = 2)
            }

            SectionHeader(stringResource(R.string.alerts))
            Toggle(stringResource(R.string.alerts_new), stringResource(R.string.alerts_new_sub), alerts) {
                alerts = it; s.alerts = it
                if (it) {
                    if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if (!s.demo) CheckWorker.schedule(ctx)
                } else CheckWorker.cancel(ctx)
            }
            Toggle(stringResource(R.string.alerts_sent_later), stringResource(R.string.alerts_sent_later_sub), sentAlerts, Modifier.testTag("sentAlerts")) { sentAlerts = it; s.sentAlerts = it }
            if (Build.VERSION.SDK_INT >= 30)
                Toggle(stringResource(R.string.alerts_chat_heads), stringResource(R.string.alerts_chat_heads_sub), chatHeads, Modifier.testTag("chatHeads")) { chatHeads = it; s.chatHeads = it }
            Stepper(stringResource(R.string.morning_time), morning) { morning = it; s.morning = it; Appearance.morning = it }
            Toggle(stringResource(R.string.quiet_hours), stringResource(R.string.quiet_sub, hour(from), hour(to)), quietOn) { quietOn = it; s.quietOn = it }
            if (quietOn) {
                Stepper(stringResource(R.string.quiet_from), from) { from = it; s.quietFrom = it }
                Stepper(stringResource(R.string.quiet_to), to) { to = it; s.quietTo = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!s.demo) OutlinedButton(onClick = {
                    scope.launch { note = runCatching { CheckWorker.check(ctx); ctx.getString(R.string.checked_ok) }.getOrElse { it.message } }
                }) { Text(stringResource(R.string.check_now)) }
                OutlinedButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }) { Text(stringResource(R.string.sound_vibration)) }
            }
            note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("note")) }
            Toggle(stringResource(R.string.buzz), stringResource(R.string.buzz_sub), Appearance.buzz, Modifier.testTag("buzz")) {
                Appearance.buzz = it; s.buzz = it
            }

            SectionHeader(stringResource(R.string.privacy))
            Toggle(stringResource(R.string.link_previews), stringResource(R.string.link_previews_sub), Appearance.linkPreviews, Modifier.testTag("linkPreviews")) {
                Appearance.linkPreviews = it; s.linkPreviews = it
            }
            Toggle(stringResource(R.string.show_typing), stringResource(R.string.show_typing_sub), Appearance.showTyping, Modifier.testTag("showTyping")) {
                Appearance.showTyping = it; s.showTyping = it
            }
            Toggle(stringResource(R.string.lock), stringResource(R.string.lock_sub), lockOn) {
                if (it && !Lock.available(ctx)) note = ctx.getString(R.string.lock_unavailable)
                else { lockOn = it; s.lock = it }
            }
            // AII-234: how long away before it asks again.
            if (lockOn) {
                Text(stringResource(R.string.lock_after), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Lock.AFTER_CHOICES.forEach { n ->
                        FilterChip(selected = lockAfter == n, onClick = { lockAfter = n; s.lockAfter = n },
                            label = { Text(stringResource(when (n) { 0 -> R.string.lock_now; 60 -> R.string.lock_1m; else -> R.string.lock_5m })) },
                            modifier = Modifier.testTag("lockAfter$n"))
                    }
                }
            }

            SectionHeader(stringResource(R.string.storage))
            // AII-215: pictures and opened files saved on the phone; they load again from the board.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.saved_pictures, savedBytes?.let { SavedPictures.format(it) } ?: "…"),
                    Modifier.testTag("savedPictures"))
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.testTag("clearPictures")) { Text(stringResource(R.string.clear)) }
            }

            SectionHeader(stringResource(R.string.about))
            Text(stringResource(R.string.report_bug), style = MaterialTheme.typography.titleSmall)
            if (crash != null) Text(stringResource(R.string.crash_note),
                color = MaterialTheme.colorScheme.error)
            OutlinedTextField(bug, { bug = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.bug_hint)) }, minLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !sending && !s.demo && (bug.isNotBlank() || crash != null), onClick = {
                    sending = true
                    scope.launch {
                        bugNote = runCatching { repo.reportBug(Crash.report(bug, crash, BuildConfig.VERSION_NAME)) }.fold(
                            { id -> Crash.clear(ctx); crash = null; bug = ""; if (id != null) ctx.getString(R.string.bug_sent_as, id) else ctx.getString(R.string.bug_sent) },
                            { it.message })
                        sending = false
                    }
                }) { Text(stringResource(R.string.bug_send)) }
                if (crash != null) TextButton(onClick = { Crash.clear(ctx); crash = null }) { Text(stringResource(R.string.crash_forget)) }
            }
            if (s.demo) Text(stringResource(R.string.demo_nothing_leaves), color = MaterialTheme.colorScheme.onSurfaceVariant)
            bugNote?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {   // AII-349
                OutlinedButton(onClick = { pickExport.launch("messenger-settings.aigm") }, modifier = Modifier.weight(1f).testTag("exportSettings")) { Text(stringResource(R.string.export_settings)) }
                OutlinedButton(onClick = { pickRestore.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f).testTag("restoreSettings")) { Text(stringResource(R.string.restore_settings)) }
            }
            TextButton(onClick = { confirmReset = true }, modifier = Modifier.testTag("resetSettings")) { Text(stringResource(R.string.reset_settings)) }
            androidx.compose.material3.ListItem(headlineContent = { Text(stringResource(R.string.about)) }, supportingContent = { Text(stringResource(R.string.about_sub)) },
                modifier = Modifier.clickable(onClick = onAbout).testTag("about"))
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    backupPass?.let { exporting ->
        val close = { backupPass = null; passText = ""; backupUri = null }
        AlertDialog(
            onDismissRequest = close,
            title = { Text(stringResource(if (exporting) R.string.export_settings else R.string.restore_settings)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(if (exporting) R.string.export_text else R.string.restore_text))
                    OutlinedTextField(passText, { passText = it }, singleLine = true, label = { Text(stringResource(R.string.passphrase)) },
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.testTag("backupPass"))
                }
            },
            confirmButton = {
                TextButton(enabled = passText.length >= 4, onClick = {
                    val uri = backupUri; val pass = passText.toCharArray(); close()
                    if (uri != null) scope.launch {
                        note = withContext(Dispatchers.IO) {
                            runCatching {
                                if (exporting) { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(s.exportSettings(pass)) }; ctx.getString(R.string.export_done) }
                                else { val n = s.importSettings(ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }, pass); ctx.getString(R.string.restore_done, n) }
                            }.getOrElse { ctx.getString(if (it is com.smizflark.aigile.messenger.data.SettingsExport.BadFile) R.string.restore_bad else R.string.backup_failed) }
                        }
                        if (!exporting) Appearance.reload(s)
                    }
                }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(stringResource(R.string.reset_settings)) },
        text = { Text(stringResource(R.string.reset_settings_text)) },
        confirmButton = {
            TextButton(onClick = {
                confirmReset = false
                s.resetSettings(); Appearance.reload(s)
                alerts = s.alerts; quietOn = s.quietOn; from = s.quietFrom; to = s.quietTo; morning = s.morning
                saved++
            }, modifier = Modifier.testTag("resetConfirm")) { Text(stringResource(R.string.reset)) }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
    )

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text(stringResource(R.string.clear_pictures_title)) },
        text = { Text(stringResource(R.string.clear_pictures_text)) },
        confirmButton = {
            TextButton(onClick = {
                confirmClear = false
                scope.launch {
                    ctx.imageLoader.memoryCache?.clear()
                    savedBytes = withContext(Dispatchers.IO) {
                        runCatching { ctx.imageLoader.diskCache?.clear() }
                        SavedPictures.clear(ctx.cacheDir, repo.store.outbox())
                    }
                }
            }) { Text(stringResource(R.string.clear)) }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
    )

    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        title = { Text(stringResource(if (s.demo) R.string.leave_demo_title else R.string.sign_out_title)) },
        text = {
            Text(stringResource(if (s.demo) R.string.leave_demo_text else R.string.sign_out_text))
        },
        confirmButton = {
            TextButton(onClick = { confirmSignOut = false; val wasDemo = s.demo; CheckWorker.cancel(ctx); repo.signOut(); onSignedOut(wasDemo) }) {
                Text(stringResource(if (s.demo) R.string.leave else R.string.sign_out_short), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
