package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatLink
import com.smizflark.aigile.messenger.data.LinkPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Link previews (AII-341): under a bubble, the first https link's page title and picture. Off by default
 * (Settings > Privacy); the phone fetches the page itself, off the main thread, and a failure shows nothing.
 */
@Composable
internal fun BubbleLinkPreview(text: String) {
    if (!Appearance.linkPreviews) return
    val url = remember(text) { LinkPreview.linkIn(text) } ?: return
    val p by produceState<LinkPreview?>(null, url) { value = withContext(Dispatchers.IO) { LinkPreview.fetch(url) } }
    p?.let { LinkCard(it) }
}

@Composable
internal fun LinkCard(p: LinkPreview) {
    val uri = LocalUriHandler.current
    val open = stringResource(R.string.link_preview_open, ChatLink.shown(p.url))
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = open) { runCatching { uri.openUri(p.url) } }.testTag("link-preview")) {
        Column {
            if (p.image != null) AsyncImage(p.image, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp))
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(p.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Text(ChatLink.shown(p.url).substringBefore('/'), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
