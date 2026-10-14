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

    /** A whole message as markdown quote lines ("> " before each line), then a blank line (AII-236). */
    fun copy(plain: String) = plain.trim('\n').lines().joinToString("\n") { "> $it" } + "\n\n"

    /** The message to send: the quote, a blank line, then the reply. */
    fun with(quote: String?, reply: String) = if (quote.isNullOrBlank()) reply else "$quote\n\n$reply"

    /**
     * Where a quote came from (AII-99): the index of the newest message older than [from] that quotes as [quoted].
     * [msgs] is newest first, null for rows that are not messages; [owner] names your own messages.
     * Falls back to the line alone, in case the name was written differently.
     */
    fun source(quoted: String, from: Int, msgs: List<Bubble?>, owner: String): Int? {
        fun line(q: String) = q.replace(Regex("^\\*\\*[^*]*:\\*\\* "), "")
        val older = msgs.indices.filter { it > from && msgs[it] != null }
        val quotes = older.associateWith { i -> msgs[i]!!.let { of(if (it.mine) owner else it.author, it.text, it.pics.size).removePrefix("> ") } }
        return older.firstOrNull { quotes[it] == quoted } ?: older.firstOrNull { line(quotes[it]!!) == line(quoted) }
    }

    /** A message as quote (the leading "> " lines, without the marks) and the rest; quote is null when there is none. */
    fun split(text: String): Pair<String?, String> {
        val lines = text.lines()
        val n = lines.takeWhile { it.startsWith(">") }.size
        if (n == 0) return null to text
        val quote = lines.take(n).joinToString("\n") { it.removePrefix(">").trimStart() }
        return quote to lines.drop(n).joinToString("\n").trim()
    }
}
