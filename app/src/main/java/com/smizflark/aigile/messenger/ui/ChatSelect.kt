package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.data.Repo
import android.os.Build
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import com.smizflark.aigile.messenger.R
import com.smizflark.aigile.messenger.data.ChatRef
import com.smizflark.aigile.messenger.data.Selection
import com.smizflark.aigile.messenger.data.Star
import com.smizflark.aigile.messenger.data.Stars
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/* ---------- select several messages (AII-371) ---------- */

/**
 * The top bar while selecting several messages: Copy, Share and Star the [picked] ones.
 * The state (select mode, the picked keys) stays in ChatScreen; [onStars] hands back the new star list.
 */
@Composable
internal fun ChatSelectBar(ref: ChatRef, repo: Repo, items: List<Row>, picked: Set<String>, stars: List<Star>, owner: String, scope: CoroutineScope,
                           onStars: (List<Star>) -> Unit, onLeave: () -> Unit) {
    val view = LocalView.current
    val colors = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    fun pickedText() = Selection.text(items.mapNotNull { (it as? Row.Msg)?.takeIf { m -> m.pending == null }?.b }, picked, ref.name, owner, { fullTime(it) },
        plain = { b -> Markdown.plain(Markdown.render(b.text, colors.surfaceVariant, colors.primary)) })
    SelectTopBar(picked.size, onLeave = onLeave,
        onCopy = {
            clipboard.setText(AnnotatedString(pickedText()))
            if (Build.VERSION.SDK_INT < 33) Toast.makeText(view.context, R.string.copied, Toast.LENGTH_SHORT).show()
            onLeave()
        },
        onShare = {
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, pickedText())
            runCatching { view.context.startActivity(android.content.Intent.createChooser(send, null)) }
            onLeave()
        },
        onStar = {
            // Stars the picked ones that have no star yet, as the single Star does (also on the board).
            var now = stars
            items.forEach { r ->
                if (r is Row.Msg && r.pending == null && r.b.key in picked && !Stars.has(now, ref.key, r.b.key)) {
                    val s = Stars.of(ref.key, r.b)
                    now = Stars.toggle(now, s); scope.launch { repo.setStar(ref, s, true) }
                }
            }
            onStars(now)
            onLeave()
        })
}
