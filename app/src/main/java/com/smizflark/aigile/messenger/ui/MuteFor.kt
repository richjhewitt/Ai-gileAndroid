package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Mute

/** Mute asks for how long (AII-157): 1 hour, 8 hours, or until I unmute. */
@Composable
fun MuteFor(onPick: (Mute.For) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("mute-for"),
        title = { Text(stringResource(R.string.mute_for)) },
        text = {
            Column {
                listOf(Mute.For.HOUR to R.string.pause_1h, Mute.For.EIGHT_HOURS to R.string.mute_8h, Mute.For.ALWAYS to R.string.mute_always).forEach { (f, label) ->
                    TextButton(onClick = { onPick(f) }, modifier = Modifier.testTag("mute-for:${f.name}")) { Text(stringResource(label)) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
