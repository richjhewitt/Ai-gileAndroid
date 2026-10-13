package com.smizflark.aigile.messenger.connect

import com.smizflark.aigile.messenger.data.BoardError
import com.smizflark.aigile.messenger.data.BoardSource
import java.net.URI
import java.net.URLDecoder

/** A board address and the phone key it gave this phone. */
data class Pairing(val url: String, val key: String)

object PairLink {
    /**
     * Reads what the QR code (or a pasted link) holds: aigile://pair?url=...&key=...
     * Returns null when it isn't a pairing link.
     */
    fun parse(text: String): Pairing? {
        val uri = runCatching { URI(text.trim()) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() != "aigile" || uri.host?.lowercase() != "pair") return null
        val q = (uri.rawQuery ?: "").split('&').mapNotNull {
            val i = it.indexOf('=')
            if (i <= 0) null else it.substring(0, i) to URLDecoder.decode(it.substring(i + 1), "UTF-8")
        }.toMap()
        val url = address(q["url"] ?: return null) ?: return null
        val key = q["key"]?.trim().orEmpty()
        if (key.isEmpty()) return null
        return Pairing(url, key)
    }

    /**
     * Tidies a typed board address: adds https:// when there is no scheme, drops spaces and the
     * trailing slash. Returns null when it can't be a web address.
     */
    fun address(typed: String): String? {
        var s = typed.trim()
        if (s.isEmpty()) return null
        if (!s.contains("://")) s = "https://$s"
        val uri = runCatching { URI(s) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
        return s.trimEnd('/')
    }

    /**
     * Signs in once to check the address and key work. Returns the board owner's name,
     * or throws [BoardError] with a message for the person holding the phone.
     */
    suspend fun check(p: Pairing, source: BoardSource = BoardSource(p.url, p.key)): String = source.owner()
}
