package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.smizflark.aigile.messenger.R
import androidx.compose.ui.res.stringResource
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import com.smizflark.aigile.messenger.data.UndoSend
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.data.Replies
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.runtime.LaunchedEffect
import com.smizflark.aigile.messenger.data.Store

/* ---------- Settings: Chats rows (AII-388 split) ---------- */

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ColumnScope.SettingsChatsRows(s: Store, rEnter: List<String>, rNext: List<String>, rRemind: List<String>, rUndo: List<String>, rQuick: List<String>, reminderCount: Int, showReplies: Boolean, onReminders: () -> Unit) {
    SettingRow(rEnter) { Toggle(stringResource(R.string.enter_sends), stringResource(R.string.enter_sends_sub), Appearance.enterSends, Modifier.testTag("enterSends")) {
        Appearance.enterSends = it; s.enterSends = it
    } }
    SettingRow(rNext) { Toggle(stringResource(R.string.next_unread_setting), stringResource(R.string.next_unread_setting_sub), Appearance.nextUnread, Modifier.testTag("nextUnread")) {
        Appearance.nextUnread = it; s.nextUnread = it
    } }
    if (reminderCount > 0) SettingRow(rRemind) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onReminders).padding(vertical = 8.dp).testTag("remindersRow"), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.reminders), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(reminderCount.toString(), color = MaterialTheme.colorScheme.primary)
        }
    }
    SettingRow(rUndo) {
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
    }
    SettingRow(rQuick) {
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
    }
}
