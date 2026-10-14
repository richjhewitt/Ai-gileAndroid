package com.smizflark.aigile.messenger.data

import java.net.URI

/**
 * GIF links in a message play in its bubble, like on the board (AII-188, the board's gifLink):
 * https only, a .gif address or a giphy/tenor media address; a giphy.com/gifs/... page becomes its GIF.
 * Links inside `code` don't count. At most three per message.
 */
object Gif {
    private val url = Regex("""https://[^\s<>]+[^\s<>.,;:!?)\]]""")
    private val code = Regex("""```[\s\S]*?(```|$)|`[^`\n]*`""")
    private val giphyPage = Regex("""^/gifs/(?:[^/]*-)?([A-Za-z0-9]+)/?$""")

    /** The GIF to show for the link [raw], or null when it isn't one. */
    fun src(raw: String): String? {
        val u = runCatching { URI(raw) }.getOrNull() ?: return null
        if (u.scheme != "https") return null
        val h = u.host?.lowercase() ?: return null
        val p = u.path.orEmpty()
        if (p.endsWith(".gif", ignoreCase = true) || (Regex("^(media\\d*|i)\\.giphy\\.com$").matches(h) && "/media/" in p)) return raw
        if (Regex("^(www\\.)?giphy\\.com$").matches(h)) giphyPage.find(p)?.let { return "https://media.giphy.com/media/${it.groupValues[1]}/giphy.gif" }
        return null
    }

    /** The GIFs a message's [text] links to, in order, each once. */
    fun inText(text: String): List<String> =
        if ("https://" !in text) emptyList()
        else url.findAll(code.replace(text, " ")).mapNotNull { src(it.value) }.distinct().take(3).toList()

    /** The GIFs as pictures for the bubble: loaded straight from the web, never with the board's key. */
    fun pics(text: String): List<Pic> = inText(text).map { Pic(it, "GIF", "image/gif") }
}
