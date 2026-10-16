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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.BuildConfig
import com.smizflark.aigile.messenger.data.Crash
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.launch

/* ---------- Settings: About rows, bug report, backup (AII-388 split) ---------- */

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ColumnScope.SettingsAboutRows(repo: Repo, rBug: List<String>, rBackup: List<String>, rReset: List<String>, rAbout: List<String>, backupNote: String?, onExport: () -> Unit, onRestore: () -> Unit, onReset: () -> Unit, onAbout: () -> Unit) {
    val s = repo.store
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var crash by remember { mutableStateOf(Crash.last(ctx)) }
    var bug by remember { mutableStateOf("") }
    var bugNote by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    SettingRow(rBug) {
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
    }
    SettingRow(rBackup) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {   // AII-349
        OutlinedButton(onClick = { onExport() }, modifier = Modifier.weight(1f).testTag("exportSettings")) { Text(stringResource(R.string.export_settings)) }
        OutlinedButton(onClick = { onRestore() }, modifier = Modifier.weight(1f).testTag("restoreSettings")) { Text(stringResource(R.string.restore_settings)) }
    }
    backupNote?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("backupNote")) }   // right under the buttons
    }
    SettingRow(rReset) { TextButton(onClick = { onReset() }, modifier = Modifier.testTag("resetSettings")) { Text(stringResource(R.string.reset_settings)) } }
    SettingRow(rAbout) {
    androidx.compose.material3.ListItem(headlineContent = { Text(stringResource(R.string.about)) }, supportingContent = { Text(stringResource(R.string.about_sub)) },
        modifier = Modifier.clickable(onClick = onAbout).testTag("about"))
    Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
