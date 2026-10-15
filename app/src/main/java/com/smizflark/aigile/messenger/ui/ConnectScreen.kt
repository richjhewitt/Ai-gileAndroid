package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.launch

/** Journey 1: connect the phone to your board, or try the demo. */
@Composable
fun ConnectScreen(repo: Repo, prefill: Pair<String, String>?, onDone: () -> Unit) {
    var url by rememberSaveable { mutableStateOf(prefill?.first ?: repo.store.url) }
    var key by rememberSaveable { mutableStateOf(prefill?.second ?: "") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // Something copied? Only its type is checked here (no "pasted" notice); the text is read when you tap the button.
    val clipboard = LocalClipboardManager.current
    val res = androidx.compose.ui.platform.LocalContext.current.resources
    val scanPrompt = stringResource(R.string.scan_prompt)
    var hasClip by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasClip = clipboard.hasText() }

    fun connect() {
        if (url.isBlank()) { error = res.getString(R.string.connect_need_address); return }
        busy = true; error = null
        scope.launch {
            try { repo.connect(url, key); onDone() }
            catch (e: Exception) { error = e.message ?: res.getString(R.string.connect_failed) }
            busy = false
        }
    }

    val scan = androidx.activity.compose.rememberLauncherForActivityResult(ScanContract()) { r ->
        val text = r.contents ?: return@rememberLauncherForActivityResult
        val pair = Mapper.parsePairing(text)
        if (pair == null) error = res.getString(R.string.connect_bad_qr)
        else { url = pair.first; key = pair.second; connect() }
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("💬", style = MaterialTheme.typography.displayMedium)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)   // no Surface here: without a colour it is black in dark mode
        Text(stringResource(R.string.connect_tagline), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        // AII-275: the demo comes first and big; connecting a board is below it, in plain words.
        Button(
            onClick = { repo.startDemo(); onDone() },
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("try-demo"), enabled = !busy,
        ) { Text(stringResource(R.string.try_demo), style = MaterialTheme.typography.titleMedium) }
        Text(stringResource(R.string.connect_demo_note), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.connect_own_board), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)

        OutlinedButton(
            onClick = { scan.launch(ScanOptions().setPrompt(scanPrompt).setBeepEnabled(false).setOrientationLocked(false)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !busy,
        ) {
            Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.scan_qr))
        }
        if (hasClip) OutlinedButton(
            onClick = {
                val pair = pairingIn(clipboard.getText()?.text)
                if (pair == null) error = res.getString(R.string.connect_bad_link)
                else { url = pair.first; key = pair.second; connect() }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("paste-link"), enabled = !busy,
        ) {
            Icon(Icons.Default.ContentPaste, null); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.use_copied_link))
        }
        Text(stringResource(R.string.or_type), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = url, onValueChange = { url = it; error = null },
            label = { Text(stringResource(R.string.board_address)) }, placeholder = { Text("my-pc.tail1234.ts.net") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("url"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = key, onValueChange = { key = it; error = null },
            label = { Text(stringResource(R.string.phone_key)) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("key"),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
        )
        if (Mapper.insecure(Mapper.normaliseUrl(url))) Text(
            stringResource(R.string.http_warning),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("error")) }
        OutlinedButton(onClick = ::connect, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.connect))
        }
    }
}

/** A pairing link anywhere in copied text (on its own, or inside a message): address to key, or null (AII-54). */
fun pairingIn(text: String?): Pair<String, String>? {
    val link = Regex("(?i)aigile://pair\\?\\S+").find(text ?: return null)?.value ?: return null
    return Mapper.parsePairing(link.trimEnd('.', ',', ')', '>', '"', '\''))
}
