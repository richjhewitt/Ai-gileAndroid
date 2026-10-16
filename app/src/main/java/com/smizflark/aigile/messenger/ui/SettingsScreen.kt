package com.smizflark.aigile.messenger.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.smizflark.aigile.messenger.R

import androidx.compose.ui.res.stringResource

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import com.smizflark.aigile.messenger.data.SettingsFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.BuildConfig
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Repo
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
    var quietOn by remember { mutableStateOf(s.quietOn) }
    var chatHeads by remember { mutableStateOf(s.chatHeads) }   // AII-381
    var sentAlerts by remember { mutableStateOf(s.sentAlerts) }   // AII-175
    var from by remember { mutableStateOf(s.quietFrom) }
    var to by remember { mutableStateOf(s.quietTo) }
    var morning by remember { mutableStateOf(s.morning) }   // AII-208
    var note by remember { mutableStateOf<String?>(null) }
    var backupNote by remember { mutableStateOf<String?>(null) }   // AII-407: the Export / Restore result, shown under those buttons
    var saved by remember { mutableIntStateOf(0) }  // bumps after a change, so the connection text re-reads the store
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }          // AII-217
    var backupPass by remember { mutableStateOf<Boolean?>(null) }   // AII-349: true = export, false = restore; null = no dialog
    var backupUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { if (it != null) { backupUri = it; backupPass = true } }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null) { backupUri = it; backupPass = false } }
    var confirmClear by remember { mutableStateOf(false) }          // AII-215
    var savedBytes by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { savedBytes = withContext(Dispatchers.IO) { SavedPictures.size(ctx.cacheDir) } }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) note = ctx.getString(R.string.alerts_permission)
    }

    /* ---------- search labels (AII-403) ---------- */
    // AII-403: the visible strings of each row, so the search can find it (plus a few extra words people may try).
    var query by remember { mutableStateOf("") }
    fun r(vararg l: String) = l.toList()
    val signOutLabel = stringResource(if (s.demo) R.string.leave_demo else R.string.sign_out)
    val rConn = r(stringResource(R.string.connection), signOutLabel, stringResource(R.string.kw_connection))
    val rBoard = r(stringResource(if (s.demo) R.string.connect_to_board else R.string.change_board), stringResource(R.string.board_address), stringResource(R.string.phone_key), stringResource(R.string.kw_connection))
    val rLook = r(stringResource(R.string.look), stringResource(R.string.text_size), stringResource(R.string.phone_colours), stringResource(R.string.phone_colours_sub), stringResource(R.string.kw_look)) +
        Look.entries.map { stringResource(it.res) } + TextSize.entries.map { stringResource(it.res) }
    val rTimes = r(stringResource(R.string.show_times), stringResource(R.string.show_times_sub))
    val rGifs = r(stringResource(R.string.play_gifs), stringResource(R.string.play_gifs_sub))
    val rPics = r(stringResource(R.string.load_pictures), stringResource(R.string.load_pics_always), stringResource(R.string.load_pics_wifi))
    val rCompact = r(stringResource(R.string.compact_list), stringResource(R.string.compact_list_sub))
    val rMuted = r(stringResource(R.string.muted_last), stringResource(R.string.muted_last_sub))
    val rSort = r(stringResource(R.string.sort_chats), stringResource(R.string.sort_newest), stringResource(R.string.sort_az), stringResource(R.string.sort_unread))
    val rSwipeR = r(stringResource(R.string.swipe_right), swipeLabel(Appearance.swipeRight))
    val rSwipeL = r(stringResource(R.string.swipe_left), swipeLabel(Appearance.swipeLeft))
    val rEnter = r(stringResource(R.string.enter_sends), stringResource(R.string.enter_sends_sub))
    val rNext = r(stringResource(R.string.next_unread_setting), stringResource(R.string.next_unread_setting_sub))
    val reminderCount = remember { s.reminders().size }   // AII-209: only when there are any
    val rRemind = r(stringResource(R.string.reminders))
    val rUndo = r(stringResource(R.string.undo_send), stringResource(R.string.undo_send_sub), stringResource(R.string.undo_seconds))
    val rQuick = r(stringResource(R.string.quick_replies), stringResource(R.string.quick_replies_sub), stringResource(R.string.quick_replies_mine))
    val kwAlerts = stringResource(R.string.kw_alerts)
    val rAlerts = r(stringResource(R.string.alerts), stringResource(R.string.alerts_new), stringResource(R.string.alerts_new_sub), kwAlerts)
    val rSent = r(stringResource(R.string.alerts_sent_later), stringResource(R.string.alerts_sent_later_sub))
    val rHeads = r(stringResource(R.string.alerts_chat_heads), stringResource(R.string.alerts_chat_heads_sub))
    val rMorning = r(stringResource(R.string.morning_time))
    val rQuiet = r(stringResource(R.string.quiet_hours), stringResource(R.string.quiet_sub, hour(from), hour(to)), stringResource(R.string.quiet_from), stringResource(R.string.quiet_to))
    val rCheck = r(stringResource(R.string.check_now), stringResource(R.string.sound_vibration), kwAlerts)
    val rBuzz = r(stringResource(R.string.buzz), stringResource(R.string.buzz_sub), kwAlerts)
    val rPrev = r(stringResource(R.string.link_previews), stringResource(R.string.link_previews_sub))
    val rTyping = r(stringResource(R.string.show_typing), stringResource(R.string.show_typing_sub))
    val rHide = r(stringResource(R.string.hide_on_lock), stringResource(R.string.hide_on_lock_sub), stringResource(R.string.kw_privacy))
    val rShots = r(stringResource(R.string.block_shots), stringResource(R.string.block_shots_sub), stringResource(R.string.kw_privacy))
    val rLock = r(stringResource(R.string.lock), stringResource(R.string.lock_sub), stringResource(R.string.lock_after), stringResource(R.string.kw_lock))
    val rStorage = r(stringResource(R.string.storage), stringResource(R.string.clear), stringResource(R.string.kw_storage))
    val rBug = r(stringResource(R.string.report_bug), stringResource(R.string.bug_hint), stringResource(R.string.bug_send))
    val rBackup = r(stringResource(R.string.export_settings), stringResource(R.string.restore_settings), stringResource(R.string.kw_backup))
    val rReset = r(stringResource(R.string.reset_settings))
    val rAbout = r(stringResource(R.string.about), stringResource(R.string.about_sub), stringResource(R.string.version, BuildConfig.VERSION_NAME))

    /* ---------- screen: sections in SettingsLook / SettingsChats / SettingsPrivacy / SettingsAbout ---------- */
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.settings)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
      androidx.compose.runtime.CompositionLocalProvider(LocalSettingsQuery provides query) {
        Column(Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("settingsSearch"), singleLine = true,
                placeholder = { Text(stringResource(R.string.find_settings)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }, modifier = Modifier.testTag("settingsSearchClear")) {
                    Icon(Icons.Default.Close, stringResource(R.string.find_settings_clear)) } })
            val all = listOf(rConn, rBoard, rLook, rTimes, rGifs, rPics, rCompact, rMuted, rSort, rSwipeR, rSwipeL, rEnter, rNext, rUndo, rQuick,
                rAlerts, rSent, rHeads, rMorning, rQuiet, rCheck, rBuzz, rPrev, rTyping, rHide, rShots, rLock, rStorage, rBug, rBackup, rReset, rAbout) +
                (if (reminderCount > 0) listOf(rRemind) else emptyList())
            if (SettingsFind.active(query) && all.none { SettingsFind.matches(query, it) })
                Text(stringResource(R.string.find_settings_none), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("settingsNone"))
            SectionHeader(stringResource(R.string.connection), first = true, rows = listOf(rConn, rBoard))
            SettingRow(rConn) {
            key(saved) {
                Text(if (s.demo) stringResource(R.string.demo_note)
                    else stringResource(R.string.board_info, s.url, s.owner, Mapper.maskKey(s.key)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("connection"))
            }
            OutlinedButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth().testTag("signOut")) {   // AII-285: right under the connection, easy to see
                Text(signOutLabel, color = MaterialTheme.colorScheme.error)
            }
            }
            SettingRow(rBoard) {
            ChangeBoard(repo, onSaved = { owner ->
                saved++; note = ctx.getString(R.string.saved_as, owner)
                if (s.alerts) CheckWorker.schedule(ctx)
            })
            }

            SectionHeader(stringResource(R.string.look), rows = listOf(rLook, rTimes, rGifs, rPics, rCompact, rMuted, rSort, rSwipeR, rSwipeL))
            SettingsLookRows(s, rLook, rTimes, rGifs, rPics, rCompact, rMuted, rSort, rSwipeR, rSwipeL)

            SectionHeader(stringResource(R.string.chats), rows = listOf(rEnter, rNext, rUndo, rQuick) + (if (reminderCount > 0) listOf(rRemind) else emptyList()))
            SettingsChatsRows(s, rEnter, rNext, rRemind, rUndo, rQuick, reminderCount, showReplies, onReminders)

            SectionHeader(stringResource(R.string.alerts), rows = listOf(rAlerts, rSent, rMorning, rQuiet, rCheck, rBuzz) + (if (Build.VERSION.SDK_INT >= 30) listOf(rHeads) else emptyList()))
            SettingRow(rAlerts) { Toggle(stringResource(R.string.alerts_new), stringResource(R.string.alerts_new_sub), alerts) {
                alerts = it; s.alerts = it
                if (it) {
                    if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if (!s.demo) CheckWorker.schedule(ctx)
                } else CheckWorker.cancel(ctx)
            } }
            SettingRow(rSent) { Toggle(stringResource(R.string.alerts_sent_later), stringResource(R.string.alerts_sent_later_sub), sentAlerts, Modifier.testTag("sentAlerts")) { sentAlerts = it; s.sentAlerts = it } }
            if (Build.VERSION.SDK_INT >= 30) SettingRow(rHeads) {
                Toggle(stringResource(R.string.alerts_chat_heads), stringResource(R.string.alerts_chat_heads_sub), chatHeads, Modifier.testTag("chatHeads")) { chatHeads = it; s.chatHeads = it }
            }
            SettingRow(rMorning) { Stepper(stringResource(R.string.morning_time), morning) { morning = it; s.morning = it; Appearance.morning = it } }
            SettingRow(rQuiet) {
            Toggle(stringResource(R.string.quiet_hours), stringResource(R.string.quiet_sub, hour(from), hour(to)), quietOn) { quietOn = it; s.quietOn = it }
            if (quietOn) {
                Stepper(stringResource(R.string.quiet_from), from) { from = it; s.quietFrom = it }
                Stepper(stringResource(R.string.quiet_to), to) { to = it; s.quietTo = it }
            }
            }
            SettingRow(rCheck) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!s.demo) OutlinedButton(onClick = {
                    scope.launch { note = runCatching { CheckWorker.check(ctx); ctx.getString(R.string.checked_ok) }.getOrElse { it.message } }
                }) { Text(stringResource(R.string.check_now)) }
                OutlinedButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }) { Text(stringResource(R.string.sound_vibration)) }
            }
            }
            note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("note")) }
            SettingRow(rBuzz) { Toggle(stringResource(R.string.buzz), stringResource(R.string.buzz_sub), Appearance.buzz, Modifier.testTag("buzz")) {
                Appearance.buzz = it; s.buzz = it
            } }

            SectionHeader(stringResource(R.string.privacy), rows = listOf(rPrev, rTyping, rHide, rShots, rLock))
            SettingsPrivacyRows(s, rPrev, rTyping, rHide, rShots, rLock, onNote = { note = it })

            SectionHeader(stringResource(R.string.storage), rows = listOf(rStorage))
            SettingRow(rStorage) {
            // AII-215: pictures and opened files saved on the phone; they load again from the board.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.saved_pictures, savedBytes?.let { SavedPictures.format(it) } ?: "…"),
                    Modifier.testTag("savedPictures"))
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.testTag("clearPictures")) { Text(stringResource(R.string.clear)) }
            }
            }

            SectionHeader(stringResource(R.string.about), rows = listOf(rBug, rBackup, rReset, rAbout))
            SettingsAboutRows(repo, rBug, rBackup, rReset, rAbout, backupNote, onExport = { pickExport.launch("messenger-settings.aigm") }, onRestore = { pickRestore.launch(arrayOf("*/*")) }, onReset = { confirmReset = true }, onAbout = onAbout)
        }
      }
    }

    /* ---------- dialogs ---------- */
    backupPass?.let { exporting ->
        SettingsBackupDialog(exporting, backupUri, s, scope, onClose = { backupPass = null; backupUri = null }, onNote = { backupNote = it })
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
