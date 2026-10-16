package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Kind
import com.smizflark.aigile.messenger.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ---------- friend DMs ---------- */

/** AII-324: how often a friend chat tells the friend that I am typing. */
internal const val TYPING_EVERY_MS = 4_000L

/**
 * AII-324: a friend chat asks the board every 3 s who is typing and whether the safety code is checked,
 * and tells them when I type (at most every 4 s). Returns the live chat (any other kind: [ref] itself).
 */
@Composable
internal fun rememberLiveRef(repo: Repo, ref: ChatRef, text: String): ChatRef {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var live by remember(ref.key) { mutableStateOf(ref) }
    if (ref.kind == Kind.FRIEND) LaunchedEffect(ref.key) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { repo.fresh(ref)?.let { live = it }; delay(3_000) } }
    }
    val typedAtOpen = remember(ref.key) { text }
    var lastTyping by remember(ref.key) { mutableStateOf(0L) }
    if (ref.kind == Kind.FRIEND) LaunchedEffect(text) {
        val now = System.currentTimeMillis()
        if (text.isNotBlank() && text != typedAtOpen && now - lastTyping >= TYPING_EVERY_MS) { lastTyping = now; runCatching { repo.source.typing(ref) } }
    }
    return live
}

internal fun friendEdit(scope: CoroutineScope, repo: Repo, ref: ChatRef, b: Bubble, t: String, onError: (String?) -> Unit, reload: suspend () -> Unit) {
    scope.launch { runCatching { repo.source.edit(ref, b.id!!, t) }.onFailure { onError(it.message) }; reload() }   // AII-324
}

internal fun friendReact(scope: CoroutineScope, repo: Repo, ref: ChatRef, b: Bubble, e: String, onError: (String?) -> Unit, reload: suspend () -> Unit) {
    scope.launch { runCatching { repo.source.react(ref, b.id!!, e) }.onFailure { onError(it.message) }; reload() }   // AII-326
}

internal fun friendDelete(scope: CoroutineScope, repo: Repo, ref: ChatRef, b: Bubble, onError: (String?) -> Unit, reload: suspend () -> Unit) {
    scope.launch { runCatching { repo.source.delete(ref, b.id!!) }.onFailure { onError(it.message) }; reload() }
}

/** AII-333: in place of the message box while a friend's safety code is new to us. */
@Composable
internal fun SafetyChangedBar(name: String, onShow: () -> Unit, onChecked: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(12.dp).testTag("safety-changed")) {
        Text(stringResource(R.string.safety_changed, name), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onShow, modifier = Modifier.testTag("safety-show")) { Text(stringResource(R.string.safety_show)) }
            Button(onClick = onChecked, modifier = Modifier.padding(start = 8.dp).testTag("safety-accept")) { Text(stringResource(R.string.we_checked_it)) }
        }
    }
}
