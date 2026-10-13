package com.smizflark.aigile.messenger.data

/**
 * Reply to one message (AII-60). The board has no threads, so a reply carries the quoted line as markdown,
 * "> **Lead:** the line", which reads fine on the board's page too.
 */
object Quote {
    private const val MAX = 120

    /** The line a reply quotes: the message's first line of text (minus any quote it had), or a picture note. */
    fun of(author: String, text: String, pics: Int = 0): String {
        val line = split(text).second.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            ?.replace("**", "")?.removePrefix("- ")
            ?: if (pics > 0) "📷 Picture" else ""
        val cut = if (line.length > MAX) line.take(MAX).trimEnd() + "…" else line
        return "> " + (if (author.isNotBlank()) "**$author:** " else "") + cut
    }

    /** The message to send: the quote, a blank line, then the reply. */
    fun with(quote: String?, reply: String) = if (quote.isNullOrBlank()) reply else "$quote\n\n$reply"

    /** A message as quote (the leading "> " lines, without the marks) and the rest; quote is null when there is none. */
    fun split(text: String): Pair<String?, String> {
        val lines = text.lines()
        val n = lines.takeWhile { it.startsWith(">") }.size
        if (n == 0) return null to text
        val quote = lines.take(n).joinToString("\n") { it.removePrefix(">").trimStart() }
        return quote to lines.drop(n).joinToString("\n").trim()
    }
}
