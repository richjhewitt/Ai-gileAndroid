package com.smizflark.aigile.messenger.ui

import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.ScrollPlace
import com.smizflark.aigile.messenger.data.ScrollPlaces
import com.smizflark.aigile.messenger.data.Transcript
import kotlinx.coroutines.flow.first

/* ---------- remembered scroll place ---------- */

/**
 * AII-72: coming back, the chat opens where you left it (and saves the place as you scroll).
 * AII-102: more new messages than fit on screen? Open at the "New messages" line, not the bottom.
 * [onRestored] fires once the saved place has been scrolled to (or there was none).
 */
@Composable
internal fun RestoreScroll(list: LazyListState, items: List<Row>, placeKey: String, place: ScrollPlace?, openAt: String?, seen: String?, onRestored: () -> Unit) {
    val latestItems by rememberUpdatedState(items)
    val latestSeen by rememberUpdatedState(seen)
    LaunchedEffect(placeKey) {
        if (place != null) {
            snapshotFlow { latestItems.size }.first { it > 0 }
            val at = ScrollPlaces.indexOf(place, latestItems.map { it.id })
            if (at != null) list.scrollToItem(at + 1, place.offset)   // +1: the spacer at the bottom
            onRestored()
        }
        snapshotFlow { Triple(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset, latestItems.size) }.collect { (i, off, n) ->
            if (n == 0) return@collect
            if (i <= 1) ScrollPlaces.clear(placeKey)
            else (list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == i }?.key as? String)
                ?.let { ScrollPlaces.save(placeKey, ScrollPlace(it, off, latestSeen)) }
        }
    }
    LaunchedEffect(placeKey) {
        if (place != null || openAt != null) return@LaunchedEffect
        val i = snapshotFlow { latestItems.indexOfFirst { it is Row.New } }.first { it >= 0 } + 1   // +1: the spacer at the bottom
        val info = snapshotFlow { list.layoutInfo }.first { it.totalItemsCount > i && it.visibleItemsInfo.isNotEmpty() }
        if (list.firstVisibleItemIndex > 1 || info.visibleItemsInfo.any { it.index == i }) return@LaunchedEffect
        list.scrollToItem(i)
        val line = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == i }?.size ?: 0
        list.scrollBy(-(info.viewportEndOffset - info.viewportStartOffset - line).toFloat())
    }
}

/* ---------- go to date ---------- */

/** AII-193: scroll so the day row at item [i] sits at the top, its messages below it (the list runs upside down). */
internal suspend fun scrollToDayRow(list: LazyListState, i: Int) {
    if (i < 0) { list.animateScrollToItem(0); return }
    // The list runs upside down, so the day row lands at the bottom: push it up to the top so its messages show below it.
    list.scrollToItem(i + 1)   // +1: the spacer at the bottom
    val day = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == i + 1 } ?: return
    list.animateScrollBy(-(list.layoutInfo.viewportSize.height - day.size).toFloat().coerceAtLeast(0f))
}

/* ---------- day and new-messages rows ---------- */

@Composable
internal fun DayRow(label: String) = Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

@Composable
internal fun NewMessagesLine() = Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("new-line"), verticalAlignment = Alignment.CenterVertically) {
    HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.new_messages), Modifier.padding(horizontal = 10.dp), style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary)
    HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
}

/* ---------- share and save the chat ---------- */

/** AII-139: the whole chat as text through the phone's share menu. */
internal fun shareTranscript(ctx: android.content.Context, name: String, c: Conversation, owner: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
        .putExtra(android.content.Intent.EXTRA_SUBJECT, name).putExtra(android.content.Intent.EXTRA_TEXT, Transcript.of(c, owner) { fullTime(it) })
    runCatching { ctx.startActivity(android.content.Intent.createChooser(send, null)) }
}

/** AII-229: the same text, saved as a .txt in Downloads. */
internal fun saveTranscript(ctx: android.content.Context, name: String, c: Conversation, owner: String) {
    val ok = saveTextFile(ctx, Transcript.fileName(name, com.smizflark.aigile.messenger.data.Now.today()), Transcript.of(c, owner) { fullTime(it) })
    Toast.makeText(ctx, if (ok) R.string.saved_to_downloads else R.string.save_failed, Toast.LENGTH_SHORT).show()
}

/** AII-193: the Go to date dialog for a chat's [items]; lands on the picked day's row. */
@androidx.compose.runtime.Composable
internal fun GoToDateHost(items: List<Row>, list: LazyListState, scope: kotlinx.coroutines.CoroutineScope, onClose: () -> Unit) {
    val days = items.mapNotNull { (it as? Row.Day)?.id?.removePrefix("day:")?.let { d -> runCatching { java.time.LocalDate.parse(d) }.getOrNull() } }
    GoToDateDialog(days, onDismiss = onClose, onPick = { picked ->
        onClose()
        val to = com.smizflark.aigile.messenger.data.GoToDate.target(days, picked)
        val i = if (to == null) -1 else items.indexOfFirst { it.id == "day:$to" }
        scope.launch { scrollToDayRow(list, i) }
    })
}

/** AII-105: opened from a search result: scroll to that message and outline it for a moment. */
@androidx.compose.runtime.Composable
internal fun OpenAtEffect(openAt: String?, list: LazyListState, items: () -> List<Row>, flash: (String?) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(openAt) {
        if (openAt == null) return@LaunchedEffect
        val i = androidx.compose.runtime.snapshotFlow { items().indexOfFirst { it.id == openAt } }.first { it >= 0 }
        list.scrollToItem(i + 1); flash(openAt); kotlinx.coroutines.delay(1500); flash(null)   // +1: the spacer at the bottom
    }
}

/** AII-282: a card waiting for your test opens at its steps and Accept (the top), unless a saved place or a search result wins. */
@androidx.compose.runtime.Composable
internal fun OpenAtTestEffect(toTest: Boolean, list: LazyListState, plain: Boolean) {
    androidx.compose.runtime.LaunchedEffect(toTest) {
        if (toTest && plain) { androidx.compose.runtime.snapshotFlow { list.layoutInfo.totalItemsCount }.first { it > 0 }; list.scrollToItem(list.layoutInfo.totalItemsCount - 1) }
    }
}
