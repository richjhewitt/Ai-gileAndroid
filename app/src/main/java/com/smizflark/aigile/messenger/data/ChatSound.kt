package com.smizflark.aigile.messenger.data

/**
 * A chat's own alert sound (AII-373). Android fixes a channel's sound once it exists, so a chat with its own
 * sound gets its own channel; no choice means the normal channel. Mute and Silent alerts still win.
 */
object ChatSound {
    /** Stored for a chat that picked "Silent" in the ringtone picker. */
    const val NONE = "none"
    private const val SEP = '\t'
    private const val PREFIX = "chat-"

    /** The uri stored for [key], or "" for the normal sound. */
    fun of(saved: Set<String>, key: String): String =
        saved.firstOrNull { it.substringBefore(SEP) == key }?.substringAfter(SEP, "") ?: ""

    /** Saves [uri] for [key]; an empty uri goes back to the normal sound. */
    fun put(saved: Set<String>, key: String, uri: String): Set<String> =
        saved.filterNot { it.substringBefore(SEP) == key }.toSet() + (if (uri.isEmpty()) emptySet() else setOf("$key$SEP$uri"))

    /** Start of the id of every channel this chat has had (to delete the old one). */
    fun prefix(key: String) = "$PREFIX$key-"

    /** The channel id: [normal] for the normal sound, else one per chat and sound. */
    fun channelId(key: String, uri: String, normal: String): String =
        if (uri.isEmpty()) normal else prefix(key) + Integer.toHexString(uri.hashCode())

    /** Which channel this chat's alert uses. */
    fun channelFor(saved: Set<String>, key: String, normal: String) = channelId(key, of(saved, key), normal)

    /** Whether the choice is no sound at all. */
    fun isNone(uri: String) = uri == NONE
}
