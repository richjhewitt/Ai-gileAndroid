package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AssistChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.CardDetail
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.Pace
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Pause
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import java.time.Instant
import com.smizflark.aigile.messenger.data.Forward
import com.smizflark.aigile.messenger.data.Quote
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.SendState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.res.stringResource
import com.smizflark.aigile.messenger.R
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.snapshotFlow
import com.smizflark.aigile.messenger.data.ScrollPlace
import com.smizflark.aigile.messenger.data.ScrollPlaces
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalView

@Composable
internal fun Composer(
    ref: ChatRef, text: String, onText: (String) -> Unit,
    targets: List<Pair<String?, String>>, target: String?, onTarget: (String?) -> Unit,
    pics: List<String> = emptyList(), picNote: String? = null, onAttach: () -> Unit = {}, onRemovePic: (String) -> Unit = {},
    onPasteCopied: (() -> Unit)? = null,
    quote: String? = null, onClearQuote: () -> Unit = {},
    onSendLater: ((Instant) -> Unit)? = null,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
            if (ref.kind == Kind.LEAD && targets.isNotEmpty()) {
                var open by remember { mutableStateOf(false) }
                val newMessage = stringResource(R.string.new_message)
                val label = targets.firstOrNull { it.first == target && it.first != null }?.second ?: newMessage
                Box {
                    AssistChip(
                        onClick = { open = true },
                        label = { Text(stringResource(R.string.replying_to, label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.testTag("target"),
                    )
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        targets.forEach { (id, l) ->
                            DropdownMenuItem(text = { Text(if (id == null) "✏️ $newMessage" else l, maxLines = 2) }, onClick = { onTarget(id); open = false })
                        }
                    }
                }
            }
            picNote?.let { PictureNote(it) }
            if (quote != null) Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).testTag("quoting"), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { QuoteBar(Markdown.render(Quote.split(quote).first ?: quote, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary), MaterialTheme.colorScheme.onSurface) }
                IconButton(onClick = onClearQuote, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Close, stringResource(R.string.quote_cancel)) }
            }
            if (onPasteCopied != null) AssistChip(
                onClick = onPasteCopied, label = { Text(stringResource(R.string.paste_picture)) },
                leadingIcon = { Icon(Icons.Default.ContentPaste, null) }, modifier = Modifier.testTag("paste-pic"),
            )
            if (pics.isNotEmpty()) PickedPictures(pics, onRemovePic)
            // AII-179: ready-made replies while the box is empty; a tap puts one in the box, Send still sends it.
            if (Appearance.quickReplies && text.isEmpty() && pics.isEmpty()) QuickReplies(onText)
            val canSend = text.isNotBlank() || pics.isNotEmpty()
            Row(verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = onAttach, enabled = pics.size < MAX_PICS, modifier = Modifier.size(52.dp).testTag("attach")) {
                    Icon(Icons.Default.AddPhotoAlternate, stringResource(R.string.add_picture))
                }
                // Text put in from outside (a draft, dictation, Edit on Send later: AII-150) gets the cursor at its end, not the start.
                var field by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
                if (field.text != text) field = TextFieldValue(text, TextRange(text.length))
                OutlinedTextField(
                    value = field, onValueChange = { field = it; onText(it.text) }, maxLines = 6,
                    placeholder = { Text(stringResource(if (ref.kind == Kind.WATCHMAN) R.string.message_watchman else R.string.message)) },
                    trailingIcon = { MicButton { onText(Dictate.join(text, it)) } },
                    shape = RoundedCornerShape(24.dp),
                    // Enter sends (AII-126): the keyboard shows a Send key, and a hardware Enter sends too (Shift+Enter still makes a new line).
                    keyboardOptions = if (Appearance.enterSends) KeyboardOptions(imeAction = ImeAction.Send) else KeyboardOptions.Default,
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    modifier = Modifier.weight(1f).testTag("input").onPreviewKeyEvent {
                        val enter = it.key == Key.Enter || it.key == Key.NumPadEnter
                        if (!Appearance.enterSends || !enter || it.isShiftPressed) false
                        else { if (it.type == KeyEventType.KeyDown && canSend) onSend(); true }
                    },
                )
                Spacer(Modifier.size(8.dp))
                SendButton(canSend, onSend, onSendLater)
            }
        }
    }
}

/** Send: tap sends now; hold it for Send later (AII-146). Drawn like a FilledIconButton, which can't take a long press. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SendButton(canSend: Boolean, onSend: () -> Unit, onSendLater: ((Instant) -> Unit)?) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val view = LocalView.current
    Box {
        Box(Modifier.size(52.dp).clip(CircleShape).background(if (canSend) cs.primary else cs.onSurface.copy(alpha = 0.12f))
            .combinedClickable(enabled = canSend, role = Role.Button, onClick = onSend,
                onLongClickLabel = stringResource(R.string.send_later),
                onLongClick = onSendLater?.let { { if (Appearance.buzz) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); menu = true } }   // AII-227)
            .testTag("send"), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.send), tint = if (canSend) cs.onPrimary else cs.onSurface.copy(alpha = 0.38f))
        }
        if (onSendLater != null) DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.send_in_hour)) }, modifier = Modifier.testTag("send-hour"),
                onClick = { menu = false; onSendLater(Instant.ofEpochMilli(Pause.oneHour(Now.millis()))) })
            DropdownMenuItem(text = { Text(stringResource(R.string.send_morning, "${Appearance.morning}:00")) }, modifier = Modifier.testTag("send-morning"),
                onClick = { menu = false; onSendLater(Instant.ofEpochMilli(Pause.nextAt(Now.millis(), Appearance.morning, 0))) })
            // AII-147: any time, with the phone's own clock picker; a time already gone today means tomorrow.
            DropdownMenuItem(text = { Text(stringResource(R.string.send_pick_time)) }, modifier = Modifier.testTag("send-pick"), onClick = {
                menu = false
                val soon = Instant.ofEpochMilli(Pause.oneHour(Now.millis())).atZone(java.time.ZoneId.systemDefault())
                android.app.TimePickerDialog(view.context, { _, h, m -> onSendLater(Instant.ofEpochMilli(Pause.nextAt(Now.millis(), h, m))) },
                    soon.hour, soon.minute, android.text.format.DateFormat.is24HourFormat(view.context)).show()
            })
            // AII-170: a later day too: the phone's calendar, then its clock (09:00 to start).
            DropdownMenuItem(text = { Text(stringResource(R.string.send_pick_day)) }, modifier = Modifier.testTag("send-day"), onClick = {
                menu = false
                val today = Now.today()
                android.app.DatePickerDialog(view.context, { _, y, mo, d ->
                    val day = java.time.LocalDate.of(y, mo + 1, d)
                    android.app.TimePickerDialog(view.context, { _, h, m -> onSendLater(Instant.ofEpochMilli(Pause.onDay(Now.millis(), day, h, m))) },
                        9, 0, android.text.format.DateFormat.is24HourFormat(view.context)).show()
                }, today.year, today.monthValue - 1, today.dayOfMonth).apply { datePicker.minDate = Now.millis() - 1000 }.show()
            })
        }
    }
}

/** One row of ready-made replies that scrolls sideways (AII-179). */
@Composable
private fun QuickReplies(onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("quick-replies"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Appearance.replies.ifEmpty { stringArrayResource(R.array.quick_replies_list).toList() }.forEach { reply ->
            SuggestionChip(onClick = { onPick(reply) }, label = { Text(reply, maxLines = 1) })
        }
    }
}

/** Pictures on one message: plenty for screenshots, and well inside the board's 20 files a comment. */
internal const val MAX_PICS = 6
