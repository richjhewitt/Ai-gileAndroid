package com.smizflark.aigile.messenger.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Repo
import com.smizflark.aigile.messenger.data.SendState

/* ---------- remind me menu ---------- */

// AII-201: Remind me picks a time; at that time an alert shows the message and opens the chat at it.
@Composable
internal fun RemindMenu(expanded: Boolean, onDismiss: () -> Unit, setRemind: (Long) -> Unit) {
    val ctx = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val nowMs = com.smizflark.aigile.messenger.data.Now.millis()
        com.smizflark.aigile.messenger.data.Remind.choices(nowMs).forEach { w ->
            val label = when (w) {
                com.smizflark.aigile.messenger.data.Remind.When.HOUR -> R.string.remind_hour
                com.smizflark.aigile.messenger.data.Remind.When.EVENING -> R.string.remind_evening
                com.smizflark.aigile.messenger.data.Remind.When.TOMORROW -> R.string.remind_tomorrow
            }
            DropdownMenuItem(text = { Text(stringResource(label)) }, modifier = Modifier.testTag("remind-$w"), onClick = {
                onDismiss()
                setRemind(com.smizflark.aigile.messenger.data.Remind.at(com.smizflark.aigile.messenger.data.Now.millis(), w) ?: return@DropdownMenuItem)
            })
        }
        // AII-225: any day and time, like Send later's pick a day; a time already gone rolls to the next one (Pause.onDay).
        DropdownMenuItem(text = { Text(stringResource(R.string.remind_pick)) }, modifier = Modifier.testTag("remind-pick"), onClick = {
            onDismiss()
            val today = com.smizflark.aigile.messenger.data.Now.today()
            android.app.DatePickerDialog(ctx, { _, y, mo, d ->
                val day = java.time.LocalDate.of(y, mo + 1, d)
                android.app.TimePickerDialog(ctx, { _, h, m -> setRemind(com.smizflark.aigile.messenger.data.Pause.onDay(com.smizflark.aigile.messenger.data.Now.millis(), day, h, m)) },
                    9, 0, android.text.format.DateFormat.is24HourFormat(ctx)).show()
            }, today.year, today.monthValue - 1, today.dayOfMonth).apply { datePicker.minDate = com.smizflark.aigile.messenger.data.Now.millis() - 1000 }.show()
        })
    }
}

/* ---------- remind me note dialog ---------- */

// AII-244: a chosen time [at] waits for its optional note, then the reminder is saved.
@Composable
internal fun RemindNoteDialog(at: Long, ref: ChatRef, b: Bubble, shown: AnnotatedString, repo: Repo, noteText: String, onNoteText: (String) -> Unit, onSaved: () -> Unit = {}, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val save = { note: String? ->
        onDone()
        val r = com.smizflark.aigile.messenger.data.Reminder(com.smizflark.aigile.messenger.data.Remind.idOf(ref.key, b.key), ref.key, ref.name, b.key,
            Markdown.plain(shown).trim(), at, note)
        repo.store.setReminders(com.smizflark.aigile.messenger.data.Remind.add(repo.store.reminders(), r))
        com.smizflark.aigile.messenger.work.RemindWorker.schedule(ctx, r)
        onSaved()   // the bubble shows its bell line straight away (AII-251)
        Toast.makeText(ctx, ctx.getString(R.string.remind_set, com.smizflark.aigile.messenger.data.Mute.endTime(at)), Toast.LENGTH_SHORT).show()
    }
    androidx.compose.material3.AlertDialog(onDismissRequest = onDone,
        title = { Text(stringResource(R.string.remind_note_title)) },
        text = { androidx.compose.material3.OutlinedTextField(noteText, onNoteText, singleLine = true,
            label = { Text(stringResource(R.string.remind_note)) }, modifier = Modifier.testTag("remind-note")) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { save(com.smizflark.aigile.messenger.data.Remind.cleanNote(noteText)) }, modifier = Modifier.testTag("remind-note-save")) { Text(stringResource(R.string.save)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { save(null) }, modifier = Modifier.testTag("remind-note-skip")) { Text(stringResource(R.string.skip)) } })
}

/* ---------- edit and delete (friend lines, AII-324) ---------- */

/** A small dialog with the line's text; Save sends the change (a blank text is not saved). */
@Composable
internal fun EditMessageDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var t by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_message)) },
        text = { androidx.compose.material3.OutlinedTextField(t, { t = it }, modifier = Modifier.testTag("edit-text")) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onSave(t.trim()) }, enabled = t.isNotBlank(), modifier = Modifier.testTag("edit-save")) { Text(stringResource(R.string.save)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.testTag("edit-cancel")) { Text(stringResource(R.string.cancel)) } })
}

/** Clear chat for me asks first (AII-363). */
@Composable
internal fun ClearChatDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_chat_title)) },
        text = { Text(stringResource(R.string.clear_chat_body)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onConfirm, modifier = Modifier.testTag("clear-confirm")) { Text(stringResource(R.string.clear_chat)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.testTag("clear-cancel")) { Text(stringResource(R.string.cancel)) } })
}

/** Delete asks first. */
@Composable
internal fun DeleteMessageDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_message_title)) },
        text = { Text(stringResource(R.string.delete_message_body)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onConfirm, modifier = Modifier.testTag("delete-confirm")) { Text(stringResource(R.string.delete)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.testTag("delete-cancel")) { Text(stringResource(R.string.cancel)) } })
}

/** AII-372: Info on a held message: exact sent time, and edited time when it was edited. */
@Composable
internal fun MessageInfoDialog(b: Bubble, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val lines = com.smizflark.aigile.messenger.data.MessageInfo.lines(b.at, b.edited,
        locale = java.util.Locale.getDefault(), h24 = android.text.format.DateFormat.is24HourFormat(ctx))
    androidx.compose.material3.AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.info_title)) },
        text = { androidx.compose.foundation.layout.Column {
            lines.forEach { l ->
                Text(stringResource(if (l.kind == com.smizflark.aigile.messenger.data.MessageInfo.Kind.SENT) R.string.info_sent else R.string.info_edited),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                Text(l.time, modifier = Modifier.testTag("info-" + l.kind.name.lowercase()))
            }
        } },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.testTag("info-close")) { Text(stringResource(R.string.done)) } })
}

/* ---------- hold menu ---------- */

// AII-43: long-press a bubble for Remind me, Reply, Forward, Star, Copy, Copy as quote, Select, links, Share, Read aloud.
@Composable
internal fun HoldMenu(expanded: Boolean, onDismiss: () -> Unit, b: Bubble, shown: AnnotatedString, starred: Boolean, onStar: (() -> Unit)?,
                      onReply: (Bubble) -> Unit, onForward: (Bubble) -> Unit, onRemind: () -> Unit, onSelect: () -> Unit, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null, onReact: ((String) -> Unit)? = null,
                      pinned: Boolean = false, onPin: (() -> Unit)? = null, onInfo: (() -> Unit)? = null, onPick: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        // AII-326: quick reactions on top, for friend lines.
        if (onReact != null) androidx.compose.foundation.layout.Row(Modifier.testTag("react-row").padding(horizontal = 8.dp)) {
            listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { e ->
                Text(e, style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { onDismiss(); onReact(e) }.padding(horizontal = 6.dp, vertical = 8.dp))
            }
        }
        if (b.state == SendState.SENT && !b.unverified && shown.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.remind_me)) }, modifier = Modifier.testTag("remind"),
            onClick = { onDismiss(); onRemind() })
        if (b.state == SendState.SENT && !b.unverified) DropdownMenuItem(text = { Text(stringResource(R.string.reply)) }, modifier = Modifier.testTag("reply"),
            onClick = { onDismiss(); onReply(b) })
        if (b.state == SendState.SENT && !b.unverified && b.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.forward)) }, modifier = Modifier.testTag("forward"),
            onClick = { onDismiss(); onForward(b) })
        // AII-138: star it, to find it again in the chat details.
        if (onStar != null && b.state == SendState.SENT && !b.unverified) DropdownMenuItem(text = { Text(stringResource(if (starred) R.string.unstar else R.string.star)) },
            modifier = Modifier.testTag("star"), onClick = { onDismiss(); onStar() })
        // AII-324: change or take back one of my own friend lines.
        if (onEdit != null) DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, modifier = Modifier.testTag("edit-msg"), onClick = { onDismiss(); onEdit() })
        if (onDelete != null) DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, modifier = Modifier.testTag("delete-msg"), onClick = { onDismiss(); onDelete() })
        // AII-339: pin it to the bar at the top of the chat.
        if (onPin != null && b.state == SendState.SENT && !b.unverified) DropdownMenuItem(text = { Text(stringResource(if (pinned) R.string.unpin_message else R.string.pin_message)) },
            modifier = Modifier.testTag("pin"), onClick = { onDismiss(); onPin() })
        // AII-372: when it was sent and edited.
        if (onInfo != null && b.at.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.info)) }, modifier = Modifier.testTag("info"), onClick = { onDismiss(); onInfo() })
        // AII-371: pick this message and more, to copy, share or star together.
        if (onPick != null) DropdownMenuItem(text = { Text(stringResource(R.string.select_message)) }, modifier = Modifier.testTag("select-msg"), onClick = { onDismiss(); onPick() })
        DropdownMenuItem(text = { Text(stringResource(R.string.copy)) }, modifier = Modifier.testTag("copy"), onClick = {
            onDismiss()
            clipboard.setText(AnnotatedString(Markdown.plain(shown)))
            // Android 13+ shows its own "copied" note; a toast as well would say it twice.
            if (Build.VERSION.SDK_INT < 33) Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.copy_quote)) }, modifier = Modifier.testTag("copy-quote"), onClick = {
            onDismiss()
            clipboard.setText(AnnotatedString(com.smizflark.aigile.messenger.data.Quote.copy(Markdown.plain(shown))))
            if (Build.VERSION.SDK_INT < 33) Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
        })
        if (shown.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.select_text)) }, modifier = Modifier.testTag("select"),
            onClick = { onDismiss(); onSelect() })
        // AII-129: copy a link in the message on its own; several links get one line each.
        val links = Markdown.links(shown)
        links.forEach { url ->
            DropdownMenuItem(text = { Text(if (links.size == 1) stringResource(R.string.copy_link) else stringResource(R.string.copy_link_named, Markdown.shortLink(url)), maxLines = 1) },
                modifier = Modifier.testTag("copy-link"), onClick = {
                    onDismiss()
                    clipboard.setText(AnnotatedString(url))
                    if (Build.VERSION.SDK_INT < 33) Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
                })
        }
        // AII-121: out to another app (email, notes...) through the phone's share menu.
        if (shown.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.share)) }, modifier = Modifier.testTag("share"), onClick = {
            onDismiss()
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, Markdown.plain(shown))
            runCatching { ctx.startActivity(android.content.Intent.createChooser(send, null)) }
        })
        // AII-394: put it in the calendar app; hidden when no app takes it.
        val plain = Markdown.plain(shown)
        if (plain.isNotBlank()) {
            val cal = com.smizflark.aigile.messenger.data.EventGuess.intent(plain, com.smizflark.aigile.messenger.data.Now.millis())
            if (ctx.packageManager.queryIntentActivities(cal, 0).isNotEmpty()) DropdownMenuItem(text = { Text(stringResource(R.string.add_to_calendar)) },
                modifier = Modifier.testTag("add-calendar"), onClick = { onDismiss(); runCatching { ctx.startActivity(cal) } })
        }
        // AII-185: the phone reads the message out; the same item stops it.
        if (shown.text.isNotBlank()) {
            val reading = ReadAloud.speaking == b.key
            DropdownMenuItem(text = { Text(stringResource(if (reading) R.string.stop_reading else R.string.read_aloud)) }, modifier = Modifier.testTag("read-aloud"), onClick = {
                onDismiss()
                if (reading) ReadAloud.stop() else ReadAloud.speak(ctx, b.key, Markdown.plain(shown))
            })
        }
    }
}
