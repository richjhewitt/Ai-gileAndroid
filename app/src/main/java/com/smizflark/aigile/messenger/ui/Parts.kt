package com.smizflark.aigile.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Repo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** "#16a34a" -> Color, or null. */
fun parseColor(hex: String?): Color? = runCatching {
    val h = hex!!.removePrefix("#")
    Color(("ff" + if (h.length == 3) h.map { "$it$it" }.joinToString("") else h).toLong(16))
}.getOrNull()

/** The lead's picture from the board, else the project's emoji on its colour. */
@Composable
fun Avatar(ref: ChatRef, repo: Repo, size: Dp = 48.dp) {
    val bg = parseColor(ref.color)?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.secondaryContainer
    val fallback = @Composable {
        Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Text(ref.emoji, fontSize = (size.value * 0.45f).sp)
        }
    }
    val board = repo.boardSource
    val pic = ref.pic
    if (board == null || pic.isNullOrBlank()) { fallback(); return }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(board.url(pic))
            .addHeader("Authorization", "Bearer ${repo.store.key}").crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).clip(CircleShape),
        loading = { fallback() },
        error = { fallback() },
    )
}

private val hm = DateTimeFormatter.ofPattern("HH:mm")
private val dayMonth = DateTimeFormatter.ofPattern("d MMM")

fun instant(iso: String?): Instant? = runCatching { Instant.parse(iso) }.getOrNull()

/** Chat list time: "now", "5m", "14:02", "Yesterday", "3 Oct". */
fun shortTime(iso: String?, now: Instant = Instant.now()): String {
    val t = instant(iso) ?: return ""
    val zone = ZoneId.systemDefault()
    val mins = ChronoUnit.MINUTES.between(t, now)
    val day = t.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m"
        day == today -> hm.format(t.atZone(zone))
        day == today.minusDays(1) -> "Yesterday"
        else -> dayMonth.format(t.atZone(zone))
    }
}

fun clock(iso: String?): String = instant(iso)?.let { hm.format(it.atZone(ZoneId.systemDefault())) } ?: ""

/** Day separator in a chat: "Today", "Yesterday", "Mon 3 Oct". */
fun dayLabel(iso: String?, today: LocalDate = LocalDate.now()): String {
    val d = instant(iso)?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: return ""
    return when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> DateTimeFormatter.ofPattern("EEE d MMM").format(d)
    }
}

fun dayOf(iso: String?): LocalDate? = instant(iso)?.atZone(ZoneId.systemDefault())?.toLocalDate()
