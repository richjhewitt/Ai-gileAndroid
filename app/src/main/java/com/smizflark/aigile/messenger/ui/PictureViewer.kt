package com.smizflark.aigile.messenger.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.SubcomposeAsyncImage
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.Repo

/* ---------- full screen picture, over the whole app (AII-93) ---------- */

/** A dialog window stops where the phone's bars were, so the picture is drawn over the app itself instead. */
internal class Shown(val pics: List<Pic>, val start: Int, val repo: Repo)
internal val LocalShowPicture = staticCompositionLocalOf<((Shown) -> Unit)?> { null }

/** Wraps the app: a tapped picture opens full screen on top of everything in it. */
@Composable
fun PictureHost(content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf<Shown?>(null) }
    val show = remember { { s: Shown -> shown = s } }
    CompositionLocalProvider(LocalShowPicture provides show) {
        Box(Modifier.fillMaxSize()) {
            content()
            shown?.let { s -> key(s) { FullPicture(s.pics, s.start, s.repo) { shown = null } } }
        }
    }
}

/** Hides the status and nav bars while shown; a swipe from the edge brings them back for a moment. */
@Composable
private fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        var c = view.context
        while (c !is Activity && c is ContextWrapper) c = c.baseContext
        val window = (c as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val bars = WindowCompat.getInsetsController(window, view)
        bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        bars.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { bars.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
/** One picture, filling the screen on black; swipe to the next one (AII-87). Tap anywhere or Back to close. */
@Composable
private fun FullPicture(pics: List<Pic>, start: Int, repo: Repo, onClose: () -> Unit) {
    val pager = rememberPagerState(initialPage = start) { pics.size }
    var zoomed by remember { mutableStateOf(false) }   // AII-90: while zoomed in, a drag looks around instead of swiping
    BackHandler(onBack = onClose)
    HideSystemBars()
    // Swallows taps and drags that miss the picture, so nothing reaches the chat underneath.
    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { } }.testTag("full-pic"),
        contentAlignment = Alignment.Center) {
        HorizontalPager(pager, Modifier.fillMaxSize().testTag("full-pic-pager"), userScrollEnabled = !zoomed) { i ->
            ZoomablePicture(pics[i], repo, onTap = onClose, onZoom = { if (i == pager.currentPage) zoomed = it })
        }
        val cutout = Modifier.windowInsetsPadding(WindowInsets.displayCutout)
        if (pics.size > 1) Text("${pager.currentPage + 1} / ${pics.size}", color = Color.White, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.BottomCenter).then(cutout).padding(24.dp).testTag("full-pic-count"))
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).then(cutout).padding(16.dp)) {
            Icon(Icons.Default.Close, "Close", tint = Color.White)
        }
        val actions = rememberPictureActions()
        val p = pics[pager.currentPage]
        val req = request(p, repo)
        Row(Modifier.align(Alignment.TopStart).then(cutout).padding(16.dp)) {
            actions.save?.let { save ->
                IconButton(onClick = { save(req, p.name) }, modifier = Modifier.testTag("full-pic-save")) {
                    Icon(Icons.Default.SaveAlt, stringResource(R.string.save_picture), tint = Color.White)
                }
            }
            IconButton(onClick = { actions.share(req, p.name) }, modifier = Modifier.testTag("full-pic-share")) {
                Icon(Icons.Default.Share, stringResource(R.string.share_picture), tint = Color.White)
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
