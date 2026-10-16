package com.smizflark.aigile.messenger.data

import kotlinx.serialization.Serializable

/** A pinned message (AII-339): kept on this phone only, with a copy of its text for the bar at the top of the chat. [pinnedAt] is when you pinned it. */
@Serializable
data class PinnedMsg(val chat: String, val key: String, val text: String, val pinnedAt: Long)

/** Pin a message and show it in the bar at the top of its chat (AII-339). Pure, so it is unit tested. */
object PinnedMsgs {
    fun of(chat: String, b: Bubble, now: Long) = PinnedMsg(chat, b.key, Stars.preview(b.text), now)

    fun has(all: List<PinnedMsg>, chat: String, key: String) = all.any { it.chat == chat && it.key == key }

    /** Pins it, or takes the pin off if it had one. */
    fun toggle(all: List<PinnedMsg>, p: PinnedMsg) =
        if (has(all, p.chat, p.key)) remove(all, p.chat, p.key) else all + p

    fun remove(all: List<PinnedMsg>, chat: String, key: String) = all.filterNot { it.chat == chat && it.key == key }

    /** This chat's pins, the one pinned last first (the bar starts on it). */
    fun inChat(all: List<PinnedMsg>, chat: String) = all.filter { it.chat == chat }.sortedByDescending { it.pinnedAt }

    /** The bar's next pin when tapped: [shown] is the index now, it wraps round to the first. */
    fun next(shown: Int, count: Int) = if (count <= 0) 0 else (shown + 1) % count

    /** Keeps the bar's index inside the list after a pin was taken off. */
    fun clamp(shown: Int, count: Int) = if (count <= 0) 0 else shown.coerceIn(0, count - 1)
}
