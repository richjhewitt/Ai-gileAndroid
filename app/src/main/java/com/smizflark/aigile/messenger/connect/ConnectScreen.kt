package com.smizflark.aigile.messenger.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import com.smizflark.aigile.messenger.data.BoardError
import kotlinx.coroutines.launch

/**
 * Journey 1: connect the phone to the board. Type the address and phone key, or scan the board's QR,
 * or try the demo. [onConnected] gets a pairing that has already signed in once, plus the owner's name;
 * the caller saves it (Store.url / Store.key, sealed by Vault).
 */
@Composable
fun ConnectScreen(
    onConnected: (Pairing, String) -> Unit,
    onDemo: () -> Unit,
    initial: Pairing? = null,
    check: suspend (Pairing) -> String = { PairLink.check(it) },
) {
    var address by rememberSaveable { mutableStateOf(initial?.url ?: "") }
    var key by rememberSaveable { mutableStateOf(initial?.key ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun connect(p: Pairing) {
        busy = true; error = null
        scope.launch {
            try { onConnected(p, check(p)) }
            catch (e: BoardError) { error = e.message }
            catch (e: Exception) { error = "Couldn't sign in: ${e.message ?: "unknown problem"}" }
            finally { busy = false }
        }
    }

    val scan = rememberLauncherForActivityResult(ScanContract()) { r ->
        val text = r.contents ?: return@rememberLauncherForActivityResult
        val p = PairLink.parse(text)
        if (p == null) error = "That QR code isn't a board pairing code."
        else { address = p.url; key = p.key; connect(p) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Connect to your board", style = MaterialTheme.typography.headlineSmall)
        Text(
            "On the board, open Setup > Phone and scan its QR code. Or type the address and phone key.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = { scan.launch(ScanOptions().setPrompt("Scan the board's pairing code").setBeepEnabled(false).setOrientationLocked(false)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Scan QR")
        }

        OutlinedTextField(
            value = address, onValueChange = { address = it; error = null },
            label = { Text("Board address") }, placeholder = { Text("https://my-pc.tailnet.ts.net") },
            singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = key, onValueChange = { key = it; error = null },
            label = { Text("Phone key") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (showKey) "Hide key" else "Show key")
                }
            },
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

        OutlinedButton(
            onClick = {
                val url = PairLink.address(address)
                when {
                    url == null -> error = "That board address doesn't look right."
                    key.isBlank() -> error = "Type the phone key from the board."
                    else -> connect(Pairing(url, key.trim()))
                }
            },
            enabled = !busy, modifier = Modifier.fillMaxWidth(),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Connect")
        }

        TextButton(onClick = onDemo, enabled = !busy) { Text("Try the demo (no board needed)") }
    }
}
