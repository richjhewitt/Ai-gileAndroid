package com.smizflark.aigile.messenger.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.smizflark.aigile.messenger.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.smizflark.aigile.messenger.data.Store

/* ---------- backup passphrase ---------- */

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun SettingsBackupDialog(exporting: Boolean, uri: android.net.Uri?, s: Store, scope: kotlinx.coroutines.CoroutineScope, onClose: () -> Unit, onNote: (String) -> Unit) {
    val ctx = LocalContext.current
    var passText by remember { mutableStateOf("") }
    val close = onClose
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
                val pass = passText.toCharArray(); close()
                if (uri != null) scope.launch {
                    onNote(withContext(Dispatchers.IO) {
                        runCatching {
                            if (exporting) { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(s.exportSettings(pass)) }; ctx.getString(R.string.export_done) }
                            else { val n = s.importSettings(ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }, pass); ctx.getString(R.string.restore_done, n) }
                        }.getOrElse { ctx.getString(if (it is com.smizflark.aigile.messenger.data.SettingsExport.BadFile) R.string.restore_bad else R.string.backup_failed) }
                    })
                    if (!exporting) Appearance.reload(s)
                }
            }) { Text(stringResource(R.string.done)) }
        },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
    )
}
