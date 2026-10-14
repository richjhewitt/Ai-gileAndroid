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

    /** The line a star shows in a list: the first line of the message (markdown already rendered), without a leading bullet (AII-144). */
    fun preview(plain: String) = plain.lines().firstOrNull { it.isNotBlank() }.orEmpty().trim().removePrefix("•").trim()

    /** A lead chat's star as the board keeps it (AII-141): "fold|at", the fold being the card id or _lead (the start of the bubble key). */
    fun boardKey(s: Star) = s.key.substringBefore(':') + "|" + s.at

    /** This chat's stars as the board has them (AII-141), other chats' left alone. A board star whose message isn't loaded here is left out. */
    fun fromBoard(all: List<Star>, chat: String, board: List<String>, bubbles: List<Bubble>): List<Star> {
        val mine = all.filter { it.chat == chat }.associateBy { boardKey(it) }
        val loaded = bubbles.associateBy { it.key.substringBefore(':') + "|" + it.at }
        return all.filter { it.chat != chat } + board.distinct().mapNotNull { k -> mine[k] ?: loaded[k]?.let { of(chat, it) } }
    }

    /** The first sync of a chat (AII-162): the phone's stars and the board's together, so neither side's stars are lost. */
    fun firstSync(all: List<Star>, chat: String, board: List<String>) =
        (all.filter { it.chat == chat }.map { boardKey(it) } + board).distinct()

    /** True when the board's stars for this chat aren't the ones the phone has (AII-162); null (an older board) never is. */
    fun outOfStep(all: List<Star>, chat: String, board: List<String>?) =
        board != null && board.toSet() != all.filter { it.chat == chat }.map { boardKey(it) }.toSet()

    /** The starred messages whose words or sender have [query] in them, any case (AII-164); a blank query keeps them all. */
    fun <T> matching(list: List<Pair<T, Star>>, query: String): List<Pair<T, Star>> {
        val q = query.trim()
        return if (q.isEmpty()) list else list.filter { (_, s) -> s.text.contains(q, ignoreCase = true) || s.author.contains(q, ignoreCase = true) }
    }

    /** Every chat's stars, newest message first (AII-140); stars of chats this phone no longer has are left out, as they can't open. */
    fun everywhere(all: List<Star>, chats: List<ChatRef>): List<Pair<ChatRef, Star>> {
        val byKey = chats.associateBy { it.key }
        return all.mapNotNull { s -> byKey[s.chat]?.let { it to s } }.sortedByDescending { it.second.at }
    }
}
