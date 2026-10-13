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
 * The little markdown the board uses: "- " bullets, **bold**, `code`, ``` blocks ```, links.
 * Blank lines between bullets are squeezed to keep bubbles compact.
 */
object Markdown {
    private val inline = Regex("""\*\*(.+?)\*\*|`([^`]+)`|(https?://[^\s)>\]]+)""")

    fun render(text: String, codeBg: Color, link: Color): AnnotatedString = buildAnnotatedString {
        val lines = squeeze(text).lines()
        var inCode = false
        lines.forEachIndexed { i, raw ->
            if (raw.trimStart().startsWith("```")) { inCode = !inCode; return@forEachIndexed }
            if (inCode) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(raw) }
            } else {
                val bullet = Regex("^\\s*[-*]\\s+").find(raw)
                if (bullet != null) { append("•  "); inlines(raw.substring(bullet.range.last + 1), codeBg, link) }
                else inlines(raw, codeBg, link)
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

    private fun AnnotatedString.Builder.inlines(s: String, codeBg: Color, link: Color) {
        var at = 0
        for (m in inline.findAll(s)) {
            append(s.substring(at, m.range.first))
            val (bold, code, url) = m.destructured
            when {
                bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(bold) }
                code.isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(code) }
                else -> withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline)))) { append(url) }
            }
            at = m.range.last + 1
        }
        append(s.substring(at))
    }
}
