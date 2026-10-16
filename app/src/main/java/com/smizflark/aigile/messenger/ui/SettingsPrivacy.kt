package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.smizflark.aigile.messenger.R
import androidx.compose.ui.res.stringResource
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.data.Store

/* ---------- Settings: Privacy rows (AII-388 split) ---------- */

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ColumnScope.SettingsPrivacyRows(s: Store, rPrev: List<String>, rTyping: List<String>, rHide: List<String>, rShots: List<String>, rLock: List<String>, onNote: (String) -> Unit) {
    val ctx = LocalContext.current
    var lockOn by remember { mutableStateOf(s.lock) }
    var lockAfter by remember { mutableStateOf(s.lockAfter) }   // AII-234
    SettingRow(rPrev) { Toggle(stringResource(R.string.link_previews), stringResource(R.string.link_previews_sub), Appearance.linkPreviews, Modifier.testTag("linkPreviews")) {
        Appearance.linkPreviews = it; s.linkPreviews = it
    } }
    SettingRow(rTyping) { Toggle(stringResource(R.string.show_typing), stringResource(R.string.show_typing_sub), Appearance.showTyping, Modifier.testTag("showTyping")) {
        Appearance.showTyping = it; s.showTyping = it
    } }
    SettingRow(rHide) { Toggle(stringResource(R.string.hide_on_lock), stringResource(R.string.hide_on_lock_sub), Appearance.hideOnLock, Modifier.testTag("hideOnLock")) {
        Appearance.hideOnLock = it; s.hideOnLock = it
    } }
    SettingRow(rShots) { Toggle(stringResource(R.string.block_shots), stringResource(R.string.block_shots_sub), Appearance.blockShots, Modifier.testTag("blockShots")) {
        Appearance.blockShots = it; s.blockShots = it
        (ctx as? android.app.Activity)?.let { a -> com.smizflark.aigile.messenger.data.Secure.apply(a.window, it) }
    } }
    SettingRow(rLock) {
    Toggle(stringResource(R.string.lock), stringResource(R.string.lock_sub), lockOn) {
        if (it && !Lock.available(ctx)) onNote(ctx.getString(R.string.lock_unavailable))
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
    }
}
