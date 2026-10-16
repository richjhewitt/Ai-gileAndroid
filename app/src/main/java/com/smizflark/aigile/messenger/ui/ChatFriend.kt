package com.smizflark.aigile.messenger.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
