@file:OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smizflark.aigile.messenger.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import com.smizflark.aigile.messenger.data.Format
import com.smizflark.aigile.messenger.data.PasteContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * The message box (AII-370). A state-based text field, because only that kind tells the keyboard it can take
 * pictures: a GIF or sticker from the keyboard (or a paste) arrives in [onPictures] as uris to copy into the app.
 */
@Composable
internal fun MessageField(
    text: String, onText: (String) -> Unit, canSend: Boolean, onSend: () -> Unit,
    onPictures: (List<Uri>, List<String?>) -> Unit,
    modifier: Modifier = Modifier, placeholder: @Composable () -> Unit, trailingIcon: @Composable () -> Unit,
    onFormatter: (((String) -> Unit)?) -> Unit = {},
) {
    // Text put in from outside (a draft, dictation, Edit on Send later: AII-150) gets the cursor at its end, not the start.
    val state = rememberTextFieldState(text, androidx.compose.ui.text.TextRange(text.length))
    val latestText by rememberUpdatedState(text)
    val latestOnText by rememberUpdatedState(onText)
    // What the box last reported: an echo of it is the box's own typing coming back, not text from outside,
    // so fast typing is never overwritten by a slightly older value.
    val sent = remember { arrayOf(text) }
    LaunchedEffect(text) {
        if (text != sent[0] && state.text.toString() != text) { sent[0] = text; state.edit { replace(0, length, text); placeCursorAtEnd() } }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { if (it != latestText) { sent[0] = it; latestOnText(it) } }
    }
    // AII-374: while words are selected, hand the screen a way to wrap them (null when nothing is selected).
    val latestFormatter by rememberUpdatedState(onFormatter)
    LaunchedEffect(state) {
        snapshotFlow { !state.selection.collapsed }.collect { has ->
            latestFormatter(if (!has) null else { marker ->
                state.edit {
                    val r = Format.toggle(toString(), selection.min, selection.max, marker)
                    if (r.text != toString()) replace(0, length, r.text)
                    selection = androidx.compose.ui.text.TextRange(r.start, r.end)
                }
            })
        }
    }
    val receiver = remember(onPictures) {
        object : ReceiveContentListener {
            override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
                val desc = transferableContent.clipMetadata.clipDescription
                val mimes = (0 until desc.mimeTypeCount).map { desc.getMimeType(it) }
                if (mimes.none { PasteContent.isPicture(it) }) return transferableContent
                val uris = mutableListOf<Uri>()
                val remaining = transferableContent.consume { item -> item.uri?.also { uris += it } != null }
                onPictures(uris, mimes)
                return remaining
            }
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(24.dp)
    val color = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        state = state, modifier = modifier.contentReceiver(receiver),
        textStyle = LocalTextStyle.current.copy(color = color), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 6),
        // Enter sends (AII-126): the keyboard shows a Send key, and a hardware Enter sends too (Shift+Enter still makes a new line).
        keyboardOptions = if (Appearance.enterSends) KeyboardOptions(imeAction = ImeAction.Send) else KeyboardOptions.Default,
        onKeyboardAction = KeyboardActionHandler { if (canSend) onSend() },
        interactionSource = interaction,
        decorator = { inner ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = state.text.toString(), innerTextField = inner, enabled = true, singleLine = false,
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None, interactionSource = interaction,
                placeholder = placeholder, trailingIcon = trailingIcon,
                container = { OutlinedTextFieldDefaults.Container(enabled = true, isError = false, interactionSource = interaction, shape = shape) },
            )
        },
    )
}
