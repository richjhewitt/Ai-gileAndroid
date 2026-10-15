package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Remind
import com.smizflark.aigile.messenger.data.Reminder
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.work.RemindWorker
import java.time.Instant

/** Every reminder (AII-209, from Settings), soonest first; hold one or tap the X to Cancel it (also stops its WorkManager job). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(repo: Repo, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var all by remember { mutableStateOf(Remind.sorted(repo.store.reminders())) }
    fun cancel(r: Reminder) {
        repo.store.setReminders(Remind.cancel(repo.store.reminders(), r.id))
        RemindWorker.cancel(ctx, r.id)
        all = Remind.sorted(repo.store.reminders())
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                title = { Text(stringResource(R.string.reminders)) },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (all.isEmpty()) EmptyState(stringResource(R.string.empty_reminders))
            LazyColumn(Modifier.fillMaxSize()) {
                Remind.groups(all, Now.millis()).forEach { (g, rows) ->
                    item(key = "group-$g") {
                        Text(stringResource(when (g) {
                            Remind.Group.TODAY -> R.string.reminders_today
                            Remind.Group.TOMORROW -> R.string.reminders_tomorrow
                            Remind.Group.LATER -> R.string.reminders_later
                        }), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp).testTag("reminder-group"))
                    }
                    items(rows, key = { it.id }) { r ->
                        ReminderRow(r, onCancel = { cancel(r) })
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderRow(r: Reminder, onCancel: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { menu = true }, onLongClickLabel = stringResource(R.string.options))
                .padding(start = 16.dp, top = 6.dp, bottom = 6.dp).testTag("reminder-row"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.chatName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("⏰ " + sendTime(Instant.ofEpochMilli(r.at).toString()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(r.text.replace('\n', ' '), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onCancel, modifier = Modifier.testTag("reminder-x")) {
                Icon(Icons.Filled.Close, stringResource(R.string.cancel_reminder))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.cancel_reminder)) }, modifier = Modifier.testTag("reminder-cancel"), onClick = { menu = false; onCancel() })
        }
    }
}
