package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Cleared
import com.smizflark.aigile.messenger.data.Now
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ---------- small pieces of ChatScreen ---------- */

/** Keeps the half-typed reply (AII-42): saved a moment after typing stops, and on the way out. */
@Composable
internal fun KeepDraft(repo: Repo, ref: ChatRef, text: String) {
    LaunchedEffect(text) { delay(400); repo.store.saveDraft(ref.key, text) }
    val latest by rememberUpdatedState(text)
    DisposableEffect(ref.key) { onDispose { repo.store.saveDraft(ref.key, latest) } }
}

/** Asks the chats list every 20 s, only on screen, whether the team is at work (AII-112, AII-113). */
@Composable
internal fun WatchWorking(repo: Repo, ref: ChatRef, lifecycle: Lifecycle, onFresh: (ChatRef) -> Unit) {
    LaunchedEffect(ref.key) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { repo.fresh(ref)?.let(onFresh); delay(20_000) } }
    }
}

/** AII-117: the day of the topmost row, shown only while you scroll by hand (jumps don't show it) and a little after. */
@Composable
internal fun rememberFloatingDay(list: LazyListState, items: List<Row>): State<String?> {
    val days = remember(items) { daysAbove(items) }
    val topDay by remember(days) { derivedStateOf { list.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index }?.let { days.getOrNull(it - 1) } } }   // index 0 is the spacer
    val dragged by list.interactionSource.collectIsDraggedAsState()
    var scrolling by remember { mutableStateOf(false) }
    LaunchedEffect(dragged, list.isScrollInProgress) {
        if (dragged) scrolling = true
        else if (scrolling && !list.isScrollInProgress) { delay(1200); scrolling = false }   // a fling after the drag keeps it up
    }
    return remember { derivedStateOf { if (scrolling) topDay else null } }
}

/** The red line under the top bar when a load or send failed. */
@Composable
internal fun ErrorLine(message: String) {
    Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(10.dp).testTag("error"))
}

/** AII-363: Clear chat for me asks first, hides everything so far on this phone, and offers Undo for a few seconds. */
@Composable
internal fun ClearChatHost(
    repo: Repo, ref: ChatRef, convo: com.smizflark.aigile.messenger.data.Conversation?, clearedAt: String?, snack: SnackbarHostState,
    scope: CoroutineScope, onDismiss: () -> Unit, onCleared: (String?) -> Unit,
) {
    val res = androidx.compose.ui.platform.LocalContext.current.resources
    ClearChatDialog(onDismiss = onDismiss, onConfirm = {
        onDismiss()
        val before = clearedAt
        val mark = Cleared.mark(convo?.bubbles.orEmpty(), Now.instant())
        onCleared(mark); repo.store.setCleared(Cleared.with(repo.store.cleared(), ref.key, mark))
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            if (snack.showSnackbar(res.getString(R.string.chat_cleared), res.getString(R.string.undo), duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                onCleared(before); repo.store.setCleared(Cleared.with(repo.store.cleared(), ref.key, before))
            }
        }
    })
}
