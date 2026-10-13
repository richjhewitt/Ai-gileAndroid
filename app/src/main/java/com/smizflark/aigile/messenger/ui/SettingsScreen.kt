package com.smizflark.aigile.messenger.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.BuildConfig
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.work.CheckWorker
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.smizflark.aigile.messenger.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repo: Repo, onBack: () -> Unit, onSignedOut: () -> Unit) {
    val s = repo.store
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf(s.alerts) }
    var quietOn by remember { mutableStateOf(s.quietOn) }
    var from by remember { mutableStateOf(s.quietFrom) }
    var to by remember { mutableStateOf(s.quietTo) }
    var note by remember { mutableStateOf<String?>(null) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) note = ctx.getString(R.string.alerts_permission)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.settings)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.connection), style = MaterialTheme.typography.titleMedium)
            Text(if (s.demo) stringResource(R.string.demo_note) else stringResource(R.string.board_signed_in, s.url, s.owner),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()

            Text(stringResource(R.string.alerts), style = MaterialTheme.typography.titleMedium)
            Toggle(stringResource(R.string.alerts_new), stringResource(R.string.alerts_new_sub), alerts) {
                alerts = it; s.alerts = it
                if (it) {
                    if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    if (!s.demo) CheckWorker.schedule(ctx)
                } else CheckWorker.cancel(ctx)
            }
            Toggle(stringResource(R.string.quiet_hours), stringResource(R.string.quiet_sub, hour(from), hour(to)), quietOn) { quietOn = it; s.quietOn = it }
            if (quietOn) {
                Stepper(stringResource(R.string.from), from) { from = it; s.quietFrom = it }
                Stepper(stringResource(R.string.to), to) { to = it; s.quietTo = it }
            }
            if (!s.demo) OutlinedButton(onClick = {
                scope.launch { note = runCatching { CheckWorker.check(ctx); ctx.getString(R.string.checked_ok) }.getOrElse { it.message } }
            }) { Text(stringResource(R.string.check_now)) }
            note?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            HorizontalDivider()

            TextButton(onClick = { CheckWorker.cancel(ctx); repo.signOut(); onSignedOut() }) {
                Text(if (s.demo) stringResource(R.string.leave_demo) else stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error)
            }
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun hour(h: Int) = "%02d:00".format(h)

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = on, onCheckedChange = onChange)
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        TextButton(onClick = { onChange((value + 23) % 24) }) { Text("−") }
        Text(hour(value), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { onChange((value + 1) % 24) }) { Text("+") }
    }
}
