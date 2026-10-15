package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Repo
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.data.CardDetail

/** The board's column names, as Rich knows them. */
fun statusLabel(status: String) = Words.status[status] ?: status.replaceFirstChar { it.uppercase() }

/** AII-273: the card view of a chat's card; a card in For you to test gets Accept and Send back, then shows its new place. */
@Composable
fun CardDetails(c: CardDetail, ref: ChatRef, repo: Repo, onChanged: (CardDetail?) -> Unit) {
    val scope = rememberCoroutineScope()
    var asking by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    fun decide(accept: Boolean, text: String = "") = scope.launch {
        failed = !repo.verdict(ref, c.id, accept, text)
        if (!failed) repo.card(ref, c.id)?.let(onChanged)
    }
    CardDetails(c, onAccept = { decide(true) }, onSendBack = { asking = true }, failed = failed)
    if (asking) SendBackDialog(onSend = { asking = false; decide(false, it) }, onDismiss = { asking = false })
}

/** Send back asks for a short reason; the board needs one. */
@Composable
fun SendBackDialog(onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("send-back-dialog"),
        title = { Text(stringResource(R.string.card_send_back_title)) },
        text = { OutlinedTextField(reason, { reason = it }, placeholder = { Text(stringResource(R.string.card_send_back_hint)) },
            modifier = Modifier.fillMaxWidth().testTag("send-back-reason")) },
        confirmButton = { TextButton(onClick = { onSend(reason.trim()) }, enabled = reason.isNotBlank(), modifier = Modifier.testTag("send-back-go")) { Text(stringResource(R.string.card_send_back)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

/** The top of a card's view (AII-45): where it is on the board, what it's about, and the steps to test it. */
@Composable
fun CardDetails(c: CardDetail, onAccept: (() -> Unit)? = null, onSendBack: (() -> Unit)? = null, failed: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val copy = rememberCopy()
    Surface(color = cs.surfaceContainer, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).testTag("card-details")) {
        Column(Modifier.padding(14.dp)) {
            Text(statusLabel(c.status) + if (c.assignee.isNotBlank()) " · ${c.assignee}" else "",
                style = MaterialTheme.typography.labelMedium, color = cs.primary)
            if (c.description.isNotBlank()) Text(Markdown.render(c.description.trim(), cs.surfaceVariant, cs.primary, onCode = copy),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            if (c.testSteps.isNotBlank()) {
                Text(stringResource(R.string.card_to_test), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
                Text(Markdown.render(c.testSteps.trim(), cs.surfaceVariant, cs.primary, onCode = copy), style = MaterialTheme.typography.bodyMedium)
            }
            if (c.status == "verify" && onAccept != null && onSendBack != null) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept, modifier = Modifier.testTag("card-accept")) { Text(stringResource(R.string.card_accept)) }
                OutlinedButton(onClick = onSendBack, modifier = Modifier.testTag("card-send-back")) { Text(stringResource(R.string.card_send_back)) }
            }
            if (failed) Text(stringResource(R.string.card_verdict_failed), color = cs.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
