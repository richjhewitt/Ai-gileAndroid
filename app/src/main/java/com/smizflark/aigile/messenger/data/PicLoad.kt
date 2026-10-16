package com.smizflark.aigile.messenger.data

/** Load pictures only on Wi-Fi (AII-391): when a picture from the web or the board is fetched. */
object PicLoad {
    const val ALWAYS = "always"
    const val WIFI = "wifi"

    /** Tests set this to pretend the network is (not) metered. */
    @Volatile var meteredOverride: Boolean? = null

    fun metered(ctx: android.content.Context): Boolean = meteredOverride ?: runCatching {
        (ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).isActiveNetworkMetered
    }.getOrDefault(false)

    /** True when the picture should load now. Cached, your own and tapped pictures always do. */
    fun shouldLoad(setting: String, metered: Boolean, cached: Boolean, mine: Boolean, tapped: Boolean): Boolean =
        setting != WIFI || !metered || cached || mine || tapped

    /** The picture is in the app's disk cache already, so nothing is fetched. */
    fun cachedOnDisk(ctx: android.content.Context, key: String): Boolean = runCatching {
        coil.Coil.imageLoader(ctx).diskCache?.openSnapshot(key)?.use { true } ?: false
    }.getOrDefault(false)
}
