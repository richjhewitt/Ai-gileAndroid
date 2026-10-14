package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Mapper
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

/** The lead's picture from the board, else the project's emoji on its colour, else initials (AII-64). */
@Composable
fun Avatar(ref: ChatRef, repo: Repo, size: Dp = 48.dp) {
    val bg = parseColor(ref.color)?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.secondaryContainer
    val fallback = @Composable {
        Box(Modifier.size(size).clip(CircleShape).background(bg).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            if (ref.emoji.isNotBlank()) Text(ref.emoji, fontSize = (size.value * 0.45f).sp)
            else Text(Mapper.initials(ref.name), fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
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

/** Words for plain (non-screen) code, so it stays testable: English until App loads the phone's language from strings.xml. */
object Words {
    var now = "now"; var minutes = "%1\$dm"; var hours = "%1\$dh"; var days = "%1\$dd"; var today = "Today"; var yesterday = "Yesterday"
    var status: Map<String, String> = mapOf("backlog" to "Backlog", "todo" to "To do", "doing" to "In progress", "review" to "Team checking",
        "verify" to "For you to test", "team" to "For the team", "done" to "Done")
    var unsentFailed = "⚠️ %1\$d not sent: tap to retry"; var unsentWaiting = "⏳ %1\$d waiting to send"; var draft = "Draft: %1\$s"; var you = "You: %1\$s"; var working = "working…"
    var sendsAt = "⏰ Sends at %1\$s"

    fun load(ctx: android.content.Context) {
        now = ctx.getString(R.string.time_now); minutes = ctx.getString(R.string.time_minutes)
        hours = ctx.getString(R.string.time_hours); days = ctx.getString(R.string.time_days)
        today = ctx.getString(R.string.today); yesterday = ctx.getString(R.string.yesterday)
        status = mapOf("backlog" to R.string.status_backlog, "todo" to R.string.status_todo, "doing" to R.string.status_doing,
            "review" to R.string.status_review, "verify" to R.string.status_verify, "team" to R.string.status_team, "done" to R.string.status_done)
            .mapValues { ctx.getString(it.value) }
        unsentFailed = ctx.getString(R.string.unsent_failed); unsentWaiting = ctx.getString(R.string.unsent_waiting); draft = ctx.getString(R.string.draft_label)
        you = ctx.getString(R.string.you_label); working = ctx.getString(R.string.working); sendsAt = ctx.getString(R.string.unsent_later)
    }
}

private val hm = DateTimeFormatter.ofPattern("HH:mm")
private val dayMonth = DateTimeFormatter.ofPattern("d MMM")

fun instant(iso: String?): Instant? = runCatching { Instant.parse(iso) }.getOrNull()

/** Chat list time: "now", "5m", "14:02", "Yesterday", "3 Oct". */
fun shortTime(iso: String?, now: Instant = com.smizflark.aigile.messenger.data.Now.instant()): String {
    val t = instant(iso) ?: return ""
    val zone = ZoneId.systemDefault()
    val mins = ChronoUnit.MINUTES.between(t, now)
    val day = t.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    return when {
        mins < 1 -> Words.now
        mins < 60 -> Words.minutes.format(mins)
        day == today -> hm.format(t.atZone(zone))
        day == today.minusDays(1) -> Words.yesterday
        else -> dayMonth.format(t.atZone(zone))
    }
}

/** AII-51: the full time under a tapped bubble: "Tue 13 Oct, 11:42" (in the phone's language). */
fun fullTime(iso: String?, zone: ZoneId = ZoneId.systemDefault()): String =
    instant(iso)?.let { DateTimeFormatter.ofPattern("EEE d MMM, HH:mm").format(it.atZone(zone)) } ?: ""

/** AII-170: when a Send later message goes: "15:00" today, "Fri 09:00" this week, else "16 Oct, 09:00". */
fun sendTime(iso: String?, now: java.time.Instant = com.smizflark.aigile.messenger.data.Now.instant(), zone: ZoneId = ZoneId.systemDefault()): String {
    val t = instant(iso)?.atZone(zone) ?: return ""
    val days = ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), t.toLocalDate())
    return when {
        days <= 0 -> hm.format(t)
        days < 7 -> DateTimeFormatter.ofPattern("EEE HH:mm").format(t)
        else -> DateTimeFormatter.ofPattern("d MMM, HH:mm").format(t)
    }
}

fun clock(iso: String?): String = instant(iso)?.let { hm.format(it.atZone(ZoneId.systemDefault())) } ?: ""

/** Day separator in a chat: "Today", "Yesterday", "Mon 3 Oct". */
fun dayLabel(iso: String?, today: LocalDate = com.smizflark.aigile.messenger.data.Now.today()): String {
    val d = instant(iso)?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: return ""
    return when (d) {
        today -> Words.today
        today.minusDays(1) -> Words.yesterday
        else -> DateTimeFormatter.ofPattern("EEE d MMM").format(d)
    }
}

fun dayOf(iso: String?): LocalDate? = instant(iso)?.atZone(ZoneId.systemDefault())?.toLocalDate()

/** An empty list (AII-73): one plain line, and a button only where it helps. */
@Composable
fun EmptyState(line: String, modifier: Modifier = Modifier, button: String? = null, onButton: () -> Unit = {}) {
    androidx.compose.foundation.layout.Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (button != null) androidx.compose.material3.OutlinedButton(onClick = onButton, modifier = Modifier.padding(top = 12.dp)) { Text(button) }
    }
}

/** Copies text with a short "Copied" note (Android 13+ shows its own); for tapping a ``` block (AII-115). */
@androidx.compose.runtime.Composable
fun rememberCopy(): (String) -> Unit {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember(clipboard, ctx) { { text ->
        clipboard.setText(androidx.compose.ui.text.AnnotatedString(text))
        if (android.os.Build.VERSION.SDK_INT < 33) android.widget.Toast.makeText(ctx, com.smizflark.aigile.messenger.R.string.copied, android.widget.Toast.LENGTH_SHORT).show()
    } }
}
