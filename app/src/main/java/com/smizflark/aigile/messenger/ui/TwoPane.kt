package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Repo

/** AII-350: wide screens (tablets, unfolded foldables) show the chats list and the open chat side by side. */
fun isWide(widthDp: Float, heightDp: Float) = widthDp >= 840f || (widthDp >= 600f && widthDp > heightDp)

/** The chats list; on a wide screen the list sits left (360dp) and [selected] chat opens on the right. Phones get the plain list. */
@Composable
fun ChatsPanes(
    repo: Repo, selected: String?, onSelect: (String?) -> Unit, onWide: (Boolean) -> Unit,
    list: @Composable (onOpen: (ChatRef) -> Unit) -> Unit,
    chat: @Composable (ChatRef) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = isWide(maxWidth.value, maxHeight.value)
        LaunchedEffect(wide) { onWide(wide) }
        if (!wide) { list { onSelect(it.key) }; return@BoxWithConstraints }
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(360.dp).fillMaxHeight()) { list { onSelect(it.key) } }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val ref = selected?.let { k -> repo.store.cachedChats()?.firstOrNull { it.key == k } }
                if (ref == null) Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(stringResource(R.string.pick_a_chat), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else key(ref.key) { chat(ref) }
            }
        }
    }
}
