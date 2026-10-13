package com.smizflark.aigile.messenger.ui

import com.smizflark.aigile.messenger.data.Bubble
import com.smizflark.aigile.messenger.data.Conversation
import com.smizflark.aigile.messenger.data.Mapper
import com.smizflark.aigile.messenger.data.News
import com.smizflark.aigile.messenger.data.Pending
import com.smizflark.aigile.messenger.data.Pic
import com.smizflark.aigile.messenger.data.SendState
import java.io.File

/** Messages from them after the newest one you saw at the bottom of the chat (AII-46). */
internal fun unreadBelow(bubbles: List<Bubble>, seen: String?): Int {
    val at = bubbles.indexOfLast { it.key == seen }
    if (seen == null || at < 0) return 0
    return bubbles.subList(at + 1, bubbles.size).count { !it.mine }
}

/** Bubbles, pending messages, day lines and card headers, oldest first. */
internal fun rows(c: Conversation?, pending: List<Pending>, owner: String, focus: String? = null, seenAtOpen: String? = null): List<Row> {
    val out = mutableListOf<Row>()
    val bubbles = c?.bubbles.orEmpty().filter { focus == null || it.card == focus }
    // Answer buttons only under the newest message of a card still waiting on you.
    val optionsAt = bubbles.withIndex().filter { it.value.options.isNotEmpty() && it.value.waiting }
        .groupBy { it.value.card }.mapNotNull { (_, l) -> l.maxOf { it.index } }.toSet()
    var day: java.time.LocalDate? = null
    var card: String? = null
    val firstNew = News.firstNew(bubbles, seenAtOpen)
    bubbles.forEachIndexed { i, b ->
        val d = dayOf(b.at)
        if (d != null && d != day) { out += Row.Day("day:$d", dayLabel(b.at)); day = d; card = null }
        if (b.key == firstNew) out += Row.New
        if (b.card != null && b.card != card) {
            out += Row.Card("card:$i:${b.card}", b.card, Mapper.clean(b.cardTitle ?: ""), b.waiting)
        }
        card = b.card
        out += Row.Msg(b.key, b, showOptions = i in optionsAt)
    }
    pending.filter { focus == null || it.target == focus }.forEach { p ->
        out += Row.Msg("p:${p.id}", Bubble("p:${p.id}", owner, p.text, p.at, true, p.target,
            state = if (p.failed && !p.offline) SendState.FAILED else SendState.SENDING,   // offline ones go by themselves (AII-55)
            pics = p.files.map { Pic(File(it).toURI().toString(), File(it).name, "image/") }), p)
    }
    return out
}
