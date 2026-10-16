package com.smizflark.aigile.messenger.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

/** The card under a bubble with a web link (AII-341): the page's [title] and, when it has one, its picture. */
data class LinkPreview(val url: String, val title: String, val image: String?) {
    companion object {
        const val MAX_BYTES = 64 * 1024
        private val meta = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
        private val attr = Regex("""([a-zA-Z:_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
        private val titleTag = Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
        // Newest 100 pages, failures too (a null), so a bad page isn't asked for again while the app runs.
        private val cache = object : LinkedHashMap<String, LinkPreview?>(32, 0.75f, true) {
            override fun removeEldestEntry(e: MutableMap.MutableEntry<String, LinkPreview?>) = size > 100
        }
        private val http by lazy {
            OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).readTimeout(4, TimeUnit.SECONDS).callTimeout(8, TimeUnit.SECONDS)
                .followSslRedirects(false).build()   // https stays https
        }

        /** The link a message gets a card for: its first one, https only, and none when the message has a GIF. */
        fun linkIn(text: String): String? {
            if (Gif.inText(text).isNotEmpty()) return null
            return Links.inText(text).firstOrNull()?.url?.takeIf { it.startsWith("https://") }
        }

        /** Reads og:title / og:image (title falls back to <title>) from the start of a page; null when there is no title. */
        fun parse(html: String, url: String): LinkPreview? {
            val tags = HashMap<String, String>()
            meta.findAll(html).forEach { m ->
                val a = attr.findAll(m.value).associate { it.groupValues[1].lowercase() to it.groupValues[2].ifEmpty { it.groupValues[3] } }
                val key = (a["property"] ?: a["name"])?.lowercase() ?: return@forEach
                val content = a["content"] ?: return@forEach
                tags.putIfAbsent(key, content)
            }
            val title = (tags["og:title"]?.let { clean(it) }?.takeIf { it.isNotBlank() }
                ?: titleTag.find(html)?.groupValues?.get(1)?.let { clean(it) }?.takeIf { it.isNotBlank() }) ?: return null
            val image = (tags["og:image"] ?: tags["og:image:secure_url"] ?: tags["og:image:url"])?.let { clean(it) }
                ?.let { runCatching { URI(url).resolve(it).toString() }.getOrNull() }?.takeIf { it.startsWith("https://") }
            return LinkPreview(url, title.take(160), image)
        }

        private fun clean(s: String) = s.replace(Regex("""\s+"""), " ").trim()
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'").replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

        /** Puts a page in the cache without the network (screenshot tests). */
        fun seed(p: LinkPreview) = synchronized(cache) { cache[p.url] = p }

        /** Fetches the page itself (no board key, 64 KB at most); failures give null. Call off the main thread. */
        fun fetch(url: String, client: OkHttpClient = http): LinkPreview? {
            synchronized(cache) { if (url in cache) return cache[url] }
            val p = runCatching {
                client.newCall(Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 AIgileMessenger").header("Accept", "text/html").build()).execute().use { r ->
                    val type = r.header("Content-Type").orEmpty().lowercase()
                    if (!r.isSuccessful || (type.isNotEmpty() && "html" !in type)) null
                    else {
                        val src = r.body!!.source()
                        val buf = okio.Buffer()
                        while (buf.size < MAX_BYTES && src.read(buf, 8192) != -1L) { }
                        parse(buf.readUtf8(minOf(buf.size, MAX_BYTES.toLong())), url)
                    }
                }
            }.getOrNull()
            synchronized(cache) { cache[url] = p }
            return p
        }
    }
}
