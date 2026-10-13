package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.TextButton
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
    var hasClip by rememberSaveable { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasClip = clipboard.hasText() }

    fun connect() {
        if (url.isBlank()) { error = "Type your board's address first."; return }
        busy = true; error = null
        scope.launch {
            try { repo.connect(url, key); onDone() }
            catch (e: Exception) { error = e.message ?: "Couldn't connect." }
            busy = false
        }
    }

    val scan = androidx.activity.compose.rememberLauncherForActivityResult(ScanContract()) { r ->
        val text = r.contents ?: return@rememberLauncherForActivityResult
        val pair = Mapper.parsePairing(text)
        if (pair == null) error = "That QR code isn't a board pairing code."
        else { url = pair.first; key = pair.second; connect() }
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("💬", style = MaterialTheme.typography.displayMedium)
        Text("AI-gile Messenger", style = MaterialTheme.typography.headlineMedium)
        Text("Your team leads and friends, from your board at home.", style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { scan.launch(ScanOptions().setPrompt("Scan the QR code in the board's Setup > Phones").setBeepEnabled(false).setOrientationLocked(false)) },
            modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !busy,
        ) {
            Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.size(8.dp)); Text("Scan QR code")
        }
        if (hasClip) OutlinedButton(
            onClick = {
                val pair = pairingIn(clipboard.getText()?.text)
                if (pair == null) error = "What you copied isn't a pairing link. Copy it again from the board's Setup > Phones."
                else { url = pair.first; key = pair.second; connect() }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("paste-link"), enabled = !busy,
        ) {
            Icon(Icons.Default.ContentPaste, null); Spacer(Modifier.size(8.dp)); Text("Use copied link")
        }
        Text("Or type it in:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = url, onValueChange = { url = it; error = null },
            label = { Text("Board address") }, placeholder = { Text("my-pc.tail1234.ts.net") },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("url"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = key, onValueChange = { key = it; error = null },
            label = { Text("Phone key") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("key"),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
        )
        if (Mapper.insecure(Mapper.normaliseUrl(url))) Text(
            "⚠️ Plain http outside Tailscale sends your key unprotected. Use https or your Tailscale address.",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("error")) }
        OutlinedButton(onClick = ::connect, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Connect")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { repo.startDemo(); onDone() }, modifier = Modifier.fillMaxWidth()) {
            Text("Try the demo (no board needed)")
        }
    }
}

/** A pairing link anywhere in copied text (on its own, or inside a message): address to key, or null (AII-54). */
fun pairingIn(text: String?): Pair<String, String>? {
    val link = Regex("(?i)aigile://pair\\?\\S+").find(text ?: return null)?.value ?: return null
    return Mapper.parsePairing(link.trimEnd('.', ',', ')', '>', '"', '\''))
}
