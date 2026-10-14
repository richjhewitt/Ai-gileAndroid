package com.smizflark.aigile.messenger.data

/** A web link in a chat (AII-194): [label] is the [name] of a named link, or empty. */
data class ChatLink(val url: String, val label: String = "") {
    /** What the row shows big: the name, or the address without https://. */
    val title get() = label.ifBlank { shown(url) }
    companion object { fun shown(url: String) = url.removePrefix("https://").removePrefix("http://").trimEnd('/') }
}

/** Every web link in the chat's messages for the details sheet: newest first, each once, at most [MAX]. */
object Links {
    const val MAX = 50
    private val code = Regex("""```[\s\S]*?(```|$)|`[^`\n]*`""")
    // Same shapes as Markdown's inline regex: [name](url), http(s):// links, bare addresses like dnbloco.com.
    private val link = Regex("""\[([^\]\n]+)]\((https?://[^\s)]+)\)|(https?://[^\s<>]*[^\s<>.,;:!?)\]])""" +
        """|(?<![\w@/.:"=-])((?i:(?:[a-z0-9-]+\.)+(?:com|net|org|io|live|dev|app|co|uk|nl|be|ai|me|tv|gg|xyz|info|site|online))(?::\d+)?(?:/[^\s<>]*[^\s<>.,;:!?)\]])?)(?![\w-])""")

    /** The links in one message, in reading order (code is skipped). */
    fun inText(text: String): List<ChatLink> {
        val plain = code.replace(text, " ")
        return link.findAll(plain).map { m ->
            val g = m.groupValues
            when {
                g[2].isNotEmpty() -> ChatLink(g[2], g[1].trim())
                g[3].isNotEmpty() -> ChatLink(g[3].trimEnd('*'))
                else -> ChatLink("https://" + g[4])
            }
        }.toList()
    }

    /** [texts] oldest first, as the chat has them. */
    fun of(texts: List<String>): List<ChatLink> =
        texts.asReversed().flatMap { inText(it) }.distinctBy { it.url }.take(MAX)
}
