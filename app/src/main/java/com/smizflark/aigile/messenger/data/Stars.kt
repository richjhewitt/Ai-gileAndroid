package com.smizflark.aigile.messenger.data

import kotlinx.serialization.Serializable

/** A starred message (AII-138): kept on this phone, with a copy of its text so the list shows even before the chat loads. */
@Serializable
data class Star(val chat: String, val key: String, val author: String, val text: String, val at: String)

/** Star a message and find it again (AII-138). Pure, so it is unit tested. */
object Stars {
    fun of(chat: String, b: Bubble) = Star(chat, b.key, b.author, b.text, b.at)

    fun has(all: List<Star>, chat: String, key: String) = all.any { it.chat == chat && it.key == key }

    /** Stars it, or takes the star off if it had one. */
    fun toggle(all: List<Star>, s: Star) =
        if (has(all, s.chat, s.key)) all.filterNot { it.chat == s.chat && it.key == s.key } else all + s

    /** This chat's stars, newest message first. */
    fun inChat(all: List<Star>, chat: String) = all.filter { it.chat == chat }.sortedByDescending { it.at }

    /** Every chat's stars, newest message first (AII-140); stars of chats this phone no longer has are left out, as they can't open. */
    fun everywhere(all: List<Star>, chats: List<ChatRef>): List<Pair<ChatRef, Star>> {
        val byKey = chats.associateBy { it.key }
        return all.mapNotNull { s -> byKey[s.chat]?.let { it to s } }.sortedByDescending { it.second.at }
    }
}
