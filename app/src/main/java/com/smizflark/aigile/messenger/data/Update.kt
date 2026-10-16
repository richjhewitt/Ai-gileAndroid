package com.smizflark.aigile.messenger.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/* ---------- 'New version' bar (AII-132): the latest build on the board (AII-314) or GitHub, checked once a day ---------- */

/** A release: its build number (the APK's versionCode) and where to download it; board = a path on the paired board (AII-314). */
data class Release(val build: Int, val url: String, val board: Boolean = false, val sha256: String = "", val parts: Int = 1)

object Update {
    const val LATEST = "https://api.github.com/repos/richjhewitt/Ai-gileAndroid/releases/latest"
    /** The release page, for when a board download fails. */
    const val PAGE = "https://github.com/richjhewitt/Ai-gileAndroid/releases/latest"
    const val DAY = 24 * 60 * 60 * 1000L
    /** The background check (AII-305): a few times a day, so an update alert comes soon after a release. */
    const val EVERY = 6 * 60 * 60 * 1000L

    @Serializable private data class Asset(val name: String = "", val browser_download_url: String = "")
    @Serializable private data class Board(val build: Int = 0, val url: String = "", val sha256: String = "", val parts: Int = 1)
    @Serializable private data class Latest(val tag_name: String = "", val html_url: String = "", val assets: List<Asset> = emptyList())

    /** The build number is the number at the end of the tag ("build-322"); a tag without one is never newer. */
    fun build(tag: String): Int = Regex("""(\d+)$""").find(tag.trim())?.value?.toIntOrNull() ?: 0

    /** The release's APK (else its GitHub page), or null when the answer isn't a release. */
    fun parse(body: String): Release? = runCatching {
        val l = json.decodeFromString(Latest.serializer(), body)
        val apk = l.assets.firstOrNull { it.name.endsWith(".apk") }?.browser_download_url
        val url = apk ?: l.html_url
        if (l.tag_name.isBlank() || url.isBlank()) null else Release(build(l.tag_name), url)
    }.getOrNull()

    /** The board's GET /api/messenger/latest (AII-313): a build and a path on the board, else null. */
    fun parseBoard(body: String): Release? = runCatching {
        val b = json.decodeFromString(Board.serializer(), body)
        if (b.build <= 0 || !b.url.startsWith("/api/")) null else Release(b.build, b.url, board = true, sha256 = b.sha256.lowercase(), parts = b.parts.coerceIn(1, 200))
    }.getOrNull()

    /** AII-316: a board build piece by piece (?part=N, 1 MB each), telling [progress] 0..100 after each; one piece = the whole file. */
    suspend fun download(b: BoardSource, r: Release, progress: (Int) -> Unit = {}): ByteArray {
        if (r.parts <= 1) return b.fileBytes(r.url).also { progress(100) }
        val out = java.io.ByteArrayOutputStream()
        for (n in 0 until r.parts) {
            out.write(b.fileBytes(r.url + "?part=" + n))
            progress((n + 1) * 100 / r.parts)
        }
        return out.toByteArray()
    }

    /** AII-315: a download matches the board's SHA-256; a board that gave none can't be checked, so it passes. */
    fun matches(bytes: ByteArray, sha256: String): Boolean = sha256.isBlank() ||
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == sha256.lowercase()

    /** AII-314: the board first; GitHub only when the board has nothing newer. The higher build wins, the board on a tie. */
    fun pick(board: Release?, mine: Int, github: () -> Release?): Release? =
        if (newer(board, mine)) board else listOfNotNull(board, github()).maxByOrNull { it.build }

    /** Board first (sealed when the link is), then GitHub; null when neither answers. */
    suspend fun check(source: ChatSource, mine: Int): Release? = withContext(Dispatchers.IO) {
        val board = runCatching { (source as? BoardSource)?.latest() }.getOrNull()
        pick(board, mine) { fetch() }
    }

    fun due(now: Long, last: Long, every: Long = DAY) = now - last >= every || now < last
    fun newer(r: Release?, mine: Int) = r != null && r.build > mine

    /** One call to GitHub; null on no signal, no release yet, or any error. Blocking: call off the main thread. */
    fun fetch(http: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()): Release? =
        runCatching {
            val req = Request.Builder().url(LATEST).header("Accept", "application/vnd.github+json").build()
            http.newCall(req).execute().use { r -> if (r.isSuccessful) parse(r.body?.string() ?: "") else null }
        }.getOrNull()
}
