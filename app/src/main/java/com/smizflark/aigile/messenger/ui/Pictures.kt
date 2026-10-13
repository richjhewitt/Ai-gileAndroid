package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R

import androidx.compose.ui.res.stringResource

import android.content.ClipDescription
import android.content.ClipboardManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Repo
import java.io.File

/* ---------- pictures in chats (AII-27) ---------- */

/** Loads a message's picture: from the phone as it is, from the board with this phone's key. */
@Composable
private fun request(pic: Pic, repo: Repo): ImageRequest {
    val b = ImageRequest.Builder(LocalContext.current).crossfade(true)
    val board = repo.boardSource
    return when {
        pic.local -> b.data(pic.path)
        board != null -> b.data(board.url(pic.path)).addHeader("Authorization", "Bearer ${repo.store.key}")
        else -> b.data(pic.path)
    }.build()
}

/** The pictures on a message, inside its bubble. Tap one to see it full screen; other files show as a 📎 line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessagePictures(pics: List<Pic>, repo: Repo) {
    var open by remember { mutableStateOf<Pic?>(null) }
    val images = pics.filter { it.image }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 6.dp)) {
        pics.forEach { p ->
            if (p.image) {
                SubcomposeAsyncImage(
                    model = request(p, repo), contentDescription = p.name.ifEmpty { stringResource(R.string.picture) },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.widthIn(max = if (pics.size == 1) 260.dp else 126.dp)
                        .heightIn(min = 80.dp, max = if (pics.size == 1) 320.dp else 126.dp)
                        .clip(RoundedCornerShape(12.dp)).clickable { open = p }.testTag("pic"),
                    loading = { Box(Modifier.size(126.dp).background(MaterialTheme.colorScheme.surfaceVariant)) },
                    error = { Missing(p) },
                )
            } else Text("📎 ${p.name.ifEmpty { stringResource(R.string.file) }}", style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    open?.let { p -> FullPicture(images, images.indexOf(p).coerceAtLeast(0), repo) { open = null } }
}

@Composable
private fun Missing(p: Pic) {
    Row(Modifier.size(126.dp).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.BrokenImage, null)
        Text(" ${p.name.ifEmpty { stringResource(R.string.picture) }}", style = MaterialTheme.typography.labelSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

/** One picture, filling the screen on black; swipe to the next one (AII-87). Tap anywhere or Back to close. */
@Composable
private fun FullPicture(pics: List<Pic>, start: Int, repo: Repo, onClose: () -> Unit) {
    val pager = rememberPagerState(initialPage = start) { pics.size }
    var zoomed by remember { mutableStateOf(false) }   // AII-90: while zoomed in, a drag looks around instead of swiping
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).testTag("full-pic"), contentAlignment = Alignment.Center) {
            HorizontalPager(pager, Modifier.fillMaxSize().testTag("full-pic-pager"), userScrollEnabled = !zoomed) { i ->
                ZoomablePicture(pics[i], repo, onTap = onClose, onZoom = { if (i == pager.currentPage) zoomed = it })
            }
            if (pics.size > 1) Text("${pager.currentPage + 1} / ${pics.size}", color = Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp).testTag("full-pic-count"))
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }
    }
}

/** AII-90: pinch to zoom, drag to look around, double tap to zoom in and back out. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomablePicture(p: Pic, repo: Repo, onTap: () -> Unit, onZoom: (Boolean) -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    fun set(s: Float, o: Offset) { scale = s; offset = if (s <= 1f) Offset.Zero else o; onZoom(s > 1f) }
    val zoom = rememberTransformableState { z, pan, _ -> set((scale * z).coerceIn(1f, 5f), offset + pan * scale) }
    Box(Modifier.fillMaxSize().clipToBounds()
        .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }, onDoubleTap = { set(if (scale > 1f) 1f else 2.5f, Offset.Zero) }) }
        .transformable(zoom, canPan = { scale > 1f })) {
        SubcomposeAsyncImage(model = request(p, repo), contentDescription = p.name, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
            error = { Missing(p) })
    }
}

/** Pictures picked but not sent yet, above the message box. Each has an ✕ to take it off. */
@Composable
fun PickedPictures(files: List<String>, onRemove: (String) -> Unit) {
    LazyRow(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(files, key = { it }) { f ->
            Box {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(File(f)).build(),
                    contentDescription = stringResource(R.string.picture_to_send), contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).testTag("picked"),
                    error = { Missing(Pic(f, File(f).name)) },
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

/* ---------- a copied picture (AII-56) ---------- */

/** Is there a picture on the clipboard that hasn't been pasted yet? Reads only the clip's type and time, so Android shows no "pasted" notice. */
fun clipHasNewPicture(mimes: List<String>, stamp: Long, pastedStamp: Long) = stamp != pastedStamp && mimes.any { it.startsWith("image/") }

/** Watches the clipboard for a copied picture (a screenshot you tapped Copy on). take() reads it once and hides the offer. */
class CopiedPicture(private val cm: ClipboardManager) {
    var available by mutableStateOf(false); private set
    private var pastedStamp = -1L

    fun check() {
        val d: ClipDescription? = cm.primaryClipDescription
        available = d != null && clipHasNewPicture((0 until d.mimeTypeCount).map { d.getMimeType(it) }, d.timestamp, pastedStamp)
    }

    fun take(): Uri? {
        pastedStamp = cm.primaryClipDescription?.timestamp ?: -1L
        available = false
        val clip = cm.primaryClip ?: return null
        return (0 until clip.itemCount).firstNotNullOfOrNull { clip.getItemAt(it).uri }
    }
}

@Composable
fun rememberCopiedPicture(): CopiedPicture {
    val ctx = LocalContext.current
    val cp = remember { CopiedPicture(ctx.getSystemService(ClipboardManager::class.java)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { cp.check() }
    DisposableEffect(cp) {
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        val l = ClipboardManager.OnPrimaryClipChangedListener { cp.check() }
        cm.addPrimaryClipChangedListener(l)
        onDispose { cm.removePrimaryClipChangedListener(l) }
    }
    return cp
}
