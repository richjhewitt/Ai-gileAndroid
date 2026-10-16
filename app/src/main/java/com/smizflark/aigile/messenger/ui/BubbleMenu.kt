package com.smizflark.aigile.messenger.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/* ---------- hold menu ---------- */

// AII-43: long-press a bubble for Remind me, Reply, Forward, Star, Copy, Copy as quote, Select, links, Share, Read aloud.
@Composable
internal fun HoldMenu(expanded: Boolean, onDismiss: () -> Unit, b: Bubble, shown: AnnotatedString, starred: Boolean, onStar: (() -> Unit)?,
                      onReply: (Bubble) -> Unit, onForward: (Bubble) -> Unit, onRemind: () -> Unit, onSelect: () -> Unit,
                      pinned: Boolean = false, onPin: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (b.state == SendState.SENT && !b.unverified && shown.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.remind_me)) }, modifier = Modifier.testTag("remind"),
            onClick = { onDismiss(); onRemind() })
        if (b.state == SendState.SENT && !b.unverified) DropdownMenuItem(text = { Text(stringResource(R.string.reply)) }, modifier = Modifier.testTag("reply"),
            onClick = { onDismiss(); onReply(b) })
        if (b.state == SendState.SENT && !b.unverified && b.text.isNotBlank()) DropdownMenuItem(text = { Text(stringResource(R.string.forward)) }, modifier = Modifier.testTag("forward"),
            onClick = { onDismiss(); onForward(b) })
        // AII-138: star it, to find it again in the chat details.
        if (onStar != null && b.state == SendState.SENT && !b.unverified) DropdownMenuItem(text = { Text(stringResource(if (starred) R.string.unstar else R.string.star)) },
            modifier = Modifier.testTag("star"), onClick = { onDismiss(); onStar() })
        // AII-339: pin it to the bar at the top of the chat.
        if (onPin != null && b.state == SendState.SENT && !b.unverified) DropdownMenuItem(text = { Text(stringResource(if (pinned) R.string.unpin_message else R.string.pin_message)) },
            modifier = Modifier.testTag("pin"), onClick = { onDismiss(); onPin() })
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
