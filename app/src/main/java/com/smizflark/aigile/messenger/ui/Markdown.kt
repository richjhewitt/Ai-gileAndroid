package com.smizflark.aigile.messenger.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

/**
 * The little markdown the board uses: "- " bullets, **bold**, `code`, ``` blocks ```, links,
 * and card ids (AIG-464) that open the card in the app when it is one of the chat's (AII-28).
 * Blank lines between bullets are squeezed to keep bubbles compact.
 */
object Markdown {
    private val inline = Regex("""\*\*(.+?)\*\*|`([^`]+)`|(https?://[^\s)>\]]+)""")

    private val cardId = Regex("""\b[A-Z]{2,6}-\d+\b""")

    /** Where the card ids in [s] sit, for the ones in [known] only. */
    fun cardRefs(s: String, known: Set<String>): List<Pair<IntRange, String>> =
        if (known.isEmpty()) emptyList() else cardId.findAll(s).filter { it.value in known }.map { it.range to it.value }.toList()

    fun render(text: String, codeBg: Color, link: Color, cards: Set<String> = emptySet(), onCard: (String) -> Unit = {}): AnnotatedString = buildAnnotatedString {
        val lines = squeeze(text).lines()
        var inCode = false
        lines.forEachIndexed { i, raw ->
            if (raw.trimStart().startsWith("```")) { inCode = !inCode; return@forEachIndexed }
            if (inCode) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(raw) }
            } else {
                val bullet = Regex("^\\s*[-*]\\s+").find(raw)
                if (bullet != null) { append("•  "); inlines(raw.substring(bullet.range.last + 1), codeBg, link, cards, onCard) }
                else inlines(raw, codeBg, link, cards, onCard)
            }
            if (i < lines.lastIndex) append('\n')
        }
    }

    /** Drops blank lines between bullets and collapses runs of blank lines. */
    fun squeeze(text: String): String {
        val out = mutableListOf<String>()
        val ls = text.trim().lines()
        for ((i, l) in ls.withIndex()) {
            if (l.isBlank()) {
                val prev = out.lastOrNull() ?: continue
                val next = ls.drop(i + 1).firstOrNull { it.isNotBlank() } ?: continue
                val bullets = isBullet(prev) && isBullet(next)
                if (!bullets && out.last().isNotBlank()) out += ""
            } else out += l
        }
        return out.joinToString("\n")
    }

    private fun isBullet(l: String) = Regex("^\\s*[-*]\\s+").containsMatchIn(l)

    private fun AnnotatedString.Builder.inlines(s: String, codeBg: Color, link: Color, cards: Set<String>, onCard: (String) -> Unit) {
        var at = 0
        for (m in inline.findAll(s)) {
            plain(s.substring(at, m.range.first), link, cards, onCard)
            val (bold, code, url) = m.destructured
            when {
                bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { plain(bold, link, cards, onCard) }
                code.isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(code) }
                else -> withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline)))) { append(url) }
            }
            at = m.range.last + 1
        }
        plain(s.substring(at), link, cards, onCard)
    }

    private fun AnnotatedString.Builder.plain(s: String, link: Color, cards: Set<String>, onCard: (String) -> Unit) {
        var at = 0
        for ((r, id) in cardRefs(s, cards)) {
            append(s.substring(at, r.first))
            withLink(LinkAnnotation.Clickable(id, TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline))) { onCard(id) }) { append(id) }
            at = r.last + 1
        }
        append(s.substring(at))
    }
}
