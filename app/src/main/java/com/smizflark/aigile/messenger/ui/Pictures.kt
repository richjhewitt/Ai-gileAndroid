package com.smizflark.aigile.messenger.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Repo
import java.io.File

/* ---------- pictures in chats (AII-27) ---------- */

/** Loads a message's picture: from the phone as it is, from the board with this phone's key. */
@Composable
internal fun request(pic: Pic, repo: Repo): ImageRequest {
    val b = ImageRequest.Builder(LocalContext.current).crossfade(true)
    val board = repo.boardSource
    return when {
        pic.local || pic.web -> b.data(pic.path)
        board != null -> b.data(board.url(pic.path)).addHeader("Authorization", "Bearer ${repo.store.key}")
        else -> b.data(pic.path)
    }.build()
}

/** The pictures on a message, inside its bubble. Tap one to see it full screen, hold it to save or share (AII-104); other files show as a 📎 line. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MessagePictures(pics: List<Pic>, repo: Repo, onOpened: () -> Unit = {}) {
    val show = LocalShowPicture.current
    val images = pics.filter { it.image }
    val actions = rememberPictureActions()
    var held by remember { mutableStateOf<Pic?>(null) }
    var failed by remember { mutableStateOf(emptySet<String>()) }   // AII-188: a GIF link that isn't one just hides, like on the board
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 6.dp)) {
        pics.forEach { p ->
            if (p.path in failed) Unit
            else if (p.image) Box {
                val req = request(p, repo)
                SubcomposeAsyncImage(
                    model = req, contentDescription = p.name.ifEmpty { stringResource(R.string.picture) },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.widthIn(max = if (pics.size == 1) 260.dp else 126.dp)
                        .heightIn(min = 80.dp, max = if (pics.size == 1) 320.dp else 126.dp)
                        .clip(RoundedCornerShape(12.dp)).combinedClickable(onLongClick = { held = p }) { show?.invoke(Shown(images, images.indexOf(p).coerceAtLeast(0), repo)); onOpened() }.testTag("pic"),
                    loading = { Box(Modifier.size(126.dp).background(MaterialTheme.colorScheme.surfaceVariant)) },
                    error = { if (!p.web) Missing(p) },
                    onError = { if (p.web) failed = failed + p.path },
                )
                DropdownMenu(expanded = held == p, onDismissRequest = { held = null }) {
                    actions.save?.let { save -> DropdownMenuItem(text = { Text(stringResource(R.string.save_picture)) }, leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
                        onClick = { held = null; save(req, p.name) }, modifier = Modifier.testTag("pic-save")) }
                    DropdownMenuItem(text = { Text(stringResource(R.string.share_picture)) }, leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = { held = null; actions.share(req, p.name) }, modifier = Modifier.testTag("pic-share"))
                }
            } else if (com.smizflark.aigile.messenger.data.Voice.isAudio(p)) VoiceNote(p, repo)   // AII-331
            else FileLine(p, repo)
        }
    }
}

@Composable
internal fun Missing(p: Pic) {
    Row(Modifier.size(126.dp).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.BrokenImage, null)
        Text(" ${p.name.ifEmpty { stringResource(R.string.picture) }}", style = MaterialTheme.typography.labelSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}


/** Pictures picked but not sent yet, above the message box. Each has an ✕ to take it off. */
@Composable
fun PickedPictures(files: List<String>, onEdit: ((String) -> Unit)? = null, onRemove: (String) -> Unit) {
    LazyRow(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(files, key = { it }) { f ->
            Box {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(File(f)).build(),
                    contentDescription = stringResource(R.string.picture_to_send), contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
                        .then(if (onEdit != null) Modifier.clickable(onClickLabel = stringResource(R.string.pic_edit)) { onEdit(f) } else Modifier).testTag("picked"),   // AII-385
                    error = { Missing(Pic(f, File(f).name)) },
                    success = { Image(painter = it.painter, contentDescription = stringResource(R.string.picture_to_send), contentScale = ContentScale.Crop, modifier = Modifier.testTag("picked-loaded")) },   // tag only once the thumbnail is drawn (tests wait for it)
                )
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(24.dp).clickable { onRemove(f) }) {
                    Icon(Icons.Default.Close, stringResource(R.string.remove_picture), tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(3.dp))
                }
            }
        }
    }
}

/** Shown while a picked picture is being copied in, or if it could not be read. */
@Composable
fun PictureNote(text: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
