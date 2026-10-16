package com.smizflark.aigile.messenger.data

import java.io.File
import java.util.Locale

/** Settings > Saved pictures (AII-215): how big the saved pictures are, and which files Clear may delete. */
object SavedPictures {
    /** 0 B, 512 B, 1.5 KB, 120 MB, 1.2 GB. One decimal under 10, none above. Always a dot, so the shots never drift. */
    fun format(bytes: Long): String {
        if (bytes < 1024) return "${maxOf(bytes, 0)} B"
        var v = bytes / 1024.0
        val units = listOf("KB", "MB", "GB")
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return (if (v < 10) String.format(Locale.US, "%.1f", v) else Math.round(v).toString()) + " " + units[i]
    }

    /** Of [files] (paths), those that may go: everything except pictures still waiting in the outbox ([pending]). */
    fun removable(files: List<String>, pending: List<Pending>): List<String> {
        val keep = pending.flatMap { it.files }.map { norm(it) }.toSet()
        return files.filter { norm(it) !in keep }
    }

    private fun norm(p: String) = p.replace('\\', '/')

    /** Every file the app saved to its cache (downloaded pictures, opened files, shared copies). Outbox copies live in filesDir, not here. */
    fun files(cacheDir: File): List<File> = cacheDir.walkTopDown().filter { it.isFile }.toList()

    fun size(cacheDir: File): Long = files(cacheDir).sumOf { it.length() }

    /** Deletes what [removable] allows; returns the new size. */
    fun clear(cacheDir: File, pending: List<Pending>): Long {
        val all = files(cacheDir)
        val go = removable(all.map { it.path }, pending).toSet()
        all.filter { it.path in go }.forEach { it.delete() }
        return size(cacheDir)
    }

    /** AII-392: what one chat's saved pictures use: how many are on the phone, and their bytes. */
    data class Usage(val count: Int, val bytes: Long)

    /**
     * The cache keys (Coil's disk cache is keyed by the picture's address) of a chat's pictures that are on the board or the web.
     * Pictures on the phone itself (file: / content:, e.g. waiting in the outbox) are never in the cache, so never here.
     */
    fun chatKeys(pics: List<Pic>, urlOf: (String) -> String): List<String> =
        pics.filter { it.image && !it.local && it.path.isNotBlank() }.map { urlOf(it.path) }.distinct()

    /** Of [keys], those with a saved file ([sizeOf] gives its bytes, null = not saved); count and total size. */
    fun usage(keys: List<String>, sizeOf: (String) -> Long?): Usage {
        val sizes = keys.mapNotNull { sizeOf(it) }
        return Usage(sizes.size, sizes.sum())
    }

    /** Of [keys], the ones Clear pictures may delete: never a key of a picture still waiting in the outbox ([pending] files). */
    fun removableKeys(keys: List<String>, pending: List<Pending>): List<String> {
        val keep = pending.flatMap { it.files }.map { norm(it).removePrefix("file://") }.toSet()
        return keys.filter { norm(it).removePrefix("file://") !in keep }
    }
}
