package com.smizflark.aigile.messenger.data

import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/* ---------- 'New version' bar (AII-132): the latest GitHub release, checked once a day ---------- */

/** A release on GitHub: its build number (the APK's versionCode) and where to download it. */
data class Release(val build: Int, val url: String)

object Update {
    const val LATEST = "https://api.github.com/repos/richjhewitt/Ai-gileAndroid/releases/latest"
    const val DAY = 24 * 60 * 60 * 1000L
    /** The background check (AII-305): a few times a day, so an update alert comes soon after a release. */
    const val EVERY = 6 * 60 * 60 * 1000L

    @Serializable private data class Asset(val name: String = "", val browser_download_url: String = "")
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

    fun due(now: Long, last: Long, every: Long = DAY) = now - last >= every || now < last
    fun newer(r: Release?, mine: Int) = r != null && r.build > mine

    /** One call to GitHub; null on no signal, no release yet, or any error. Blocking: call off the main thread. */
    fun fetch(http: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()): Release? =
        runCatching {
            val req = Request.Builder().url(LATEST).header("Accept", "application/vnd.github+json").build()
            http.newCall(req).execute().use { r -> if (r.isSuccessful) parse(r.body?.string() ?: "") else null }
        }.getOrNull()
}
