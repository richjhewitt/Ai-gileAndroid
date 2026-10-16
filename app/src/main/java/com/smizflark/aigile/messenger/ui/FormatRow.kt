package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Format

/** B, I and </> just above the message box while words are selected (AII-374). */
@Composable
internal fun FormatRow(format: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp).testTag("format-row")) {
        FormatButton("B", stringResource(R.string.format_bold), "format-bold", bold = true) { format(Format.BOLD) }
        FormatButton("I", stringResource(R.string.format_italic), "format-italic", italic = true) { format(Format.ITALIC) }
        FormatButton("</>", stringResource(R.string.format_code), "format-code", mono = true) { format(Format.CODE) }
    }
}

@Composable
private fun FormatButton(label: String, description: String, tag: String, bold: Boolean = false, italic: Boolean = false, mono: Boolean = false, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.testTag(tag).semantics { contentDescription = description }) {
        Text(
            label,
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            fontFamily = if (mono) FontFamily.Monospace else null,
        )
    }
}
