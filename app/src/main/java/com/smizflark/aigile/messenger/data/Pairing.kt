package com.smizflark.aigile.messenger.data

import java.net.URI
import java.net.URLDecoder

/** A board this phone is paired with: its address and the phone key the board gave out. */
data class Pairing(val url: String, val key: String) {

    /** Asks the board who owns it: proves the address and key work. Throws [BoardError] if not. */
    suspend fun signIn(): String = BoardSource(url, key).owner()

    companion object {
        /** Reads the board's QR link: aigile://pair?url=...&key=... Null if it isn't one. */
        fun parse(link: String): Pairing? {
            val uri = runCatching { URI(link.trim()) }.getOrNull() ?: return null
            if (uri.scheme?.lowercase() != "aigile" || uri.host?.lowercase() != "pair") return null
            val q = (uri.rawQuery ?: return null).split("&").mapNotNull {
                val i = it.indexOf('=')
                if (i <= 0) null else it.substring(0, i) to URLDecoder.decode(it.substring(i + 1), "UTF-8")
            }.toMap()
            val url = address(q["url"] ?: return null) ?: return null
            val key = q["key"]?.trim().orEmpty()
            return if (key.isEmpty()) null else Pairing(url, key)
        }

        /** Tidies a typed board address: adds http:// when missing, drops the trailing slash. Null if it can't be one. */
        fun address(typed: String): String? {
            var s = typed.trim().trimEnd('/')
            if (s.isEmpty() || s.any { it.isWhitespace() }) return null
            if (!s.contains("://")) s = "http://$s"
            val uri = runCatching { URI(s) }.getOrNull() ?: return null
            if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
            return s
        }
    }
}
