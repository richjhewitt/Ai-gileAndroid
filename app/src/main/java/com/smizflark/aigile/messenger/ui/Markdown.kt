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
 * With [onCode], tapping a ``` block hands over its whole text, to copy it (AII-115).
 */
object Markdown {
    private val inline = Regex("""\*\*(.+?)\*\*|`([^`]+)`|(https?://[^\s)>\]]+)""")

    private val cardId = Regex("""\b[A-Z]{2,6}-\d+\b""")

    /** Where the card ids in [s] sit, for the ones in [known] only. */
    fun cardRefs(s: String, known: Set<String>): List<Pair<IntRange, String>> =
        if (known.isEmpty()) emptyList() else cardId.findAll(s).filter { it.value in known }.map { it.range to it.value }.toList()

    /** Rendered messages, newest use last: bubbles scrolling back into view aren't parsed again. */
    private val cache = object : LinkedHashMap<Triple<String, Color, Color>, AnnotatedString>(64, 0.75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<Triple<String, Color, Color>, AnnotatedString>) = size > 400
    }

    /** The web links a rendered message shows, first three, for Copy link in its menu (AII-129). */
    fun links(shown: AnnotatedString): List<String> =
        shown.getLinkAnnotations(0, shown.length).mapNotNull { (it.item as? LinkAnnotation.Url)?.url }.distinct().take(3)

    /** A link short enough for a menu line: no https://, at most [max] letters. */
    fun shortLink(url: String, max: Int = 32): String =
        url.removePrefix("https://").removePrefix("http://").let { if (it.length > max) it.take(max - 1) + "…" else it }

    fun render(text: String, codeBg: Color, link: Color, cards: Set<String> = emptySet(), onCard: (String) -> Unit = {}, onCode: ((String) -> Unit)? = null): AnnotatedString =
        if (cards.isEmpty() && (onCode == null || "```" !in text)) synchronized(cache) { cache.getOrPut(Triple(text, codeBg, link)) { build(text, codeBg, link, cards, onCard, null) } }
        else build(text, codeBg, link, cards, onCard, onCode)   // card and code links carry a click: not cached

    /** The text of each ``` block, by the line its opening fence is on. */
    fun codeBlocks(lines: List<String>): Map<Int, String> {
        val out = mutableMapOf<Int, String>()
        var open = -1
        lines.forEachIndexed { i, l ->
            if (!l.trimStart().startsWith("```")) return@forEachIndexed
            if (open < 0) open = i else { out[open] = lines.subList(open + 1, i).joinToString("\n"); open = -1 }
        }
        if (open >= 0) out[open] = lines.subList(open + 1, lines.size).joinToString("\n")   // never closed: runs to the end
        return out
    }

    private fun build(text: String, codeBg: Color, link: Color, cards: Set<String>, onCard: (String) -> Unit, onCode: ((String) -> Unit)?): AnnotatedString = buildAnnotatedString {
        val lines = squeeze(text).lines()
        val blocks = if (onCode != null) codeBlocks(lines) else emptyMap()
        var inCode = false
        var block = ""
        var first = true   // a newline goes before each shown line, so a closing fence leaves no blank line below
        lines.forEachIndexed { i, raw ->
            if (raw.trimStart().startsWith("```")) { inCode = !inCode; block = blocks[i] ?: ""; return@forEachIndexed }
            if (!first) append('\n')
            first = false
            if (inCode) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) {
                    if (onCode != null && raw.isNotEmpty()) { val b = block; withLink(LinkAnnotation.Clickable("code:$i") { onCode(b) }) { append(raw) } }
                    else append(raw)
                }
            } else {
                val bullet = bulletRe.find(raw)
                if (bullet != null) { append("•  "); inlines(raw.substring(bullet.range.last + 1), codeBg, link, cards, onCard) }
                else inlines(raw, codeBg, link, cards, onCard)
            }
        }
    }

    /** Drops blank lines between bullets and collapses runs of blank lines. */
    fun squeeze(text: String): String {
        val out = mutableListOf<String>()
        val ls = text.trim().lines()
        var nextAt = 0   // index of the next non-blank line, found once per run of blank lines
        for ((i, l) in ls.withIndex()) {
            if (l.isBlank()) {
                val prev = out.lastOrNull() ?: continue
                if (nextAt <= i) { nextAt = i + 1; while (nextAt < ls.size && ls[nextAt].isBlank()) nextAt++ }
                val next = ls.getOrNull(nextAt) ?: continue
                val bullets = isBullet(prev) && isBullet(next)
                if (!bullets && out.last().isNotBlank()) out += ""
            } else out += l
        }
        return out.joinToString("\n")
    }

    private val bulletRe = Regex("^\\s*[-*]\\s+")
    private fun isBullet(l: String) = bulletRe.containsMatchIn(l)

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
