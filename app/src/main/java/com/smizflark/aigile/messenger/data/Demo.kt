package com.smizflark.aigile.messenger.data

import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Try the app with no board: a few made-up chats that answer back. Nothing leaves the phone. */
class DemoSource : ChatSource {
    private fun ago(min: Long) = Instant.now().minus(min, ChronoUnit.MINUTES).toString()

    private val threads = mutableMapOf(
        "ai-gile" to WireThread(
            slug = "ai-gile", name = "AI-gile", emoji = "🗂️", color = "#16a34a", lead = "Lead",
            messages = mutableListOf(
                WireMsg("AIG-1", "💬 Phone app", "Rich", "Can I read my DMs on the phone yet?", ago(95), mine = true),
                WireMsg("AIG-1", "💬 Phone app", "Lead", "- Yes: this is the **Messenger** app\n\n- Reply here and it lands on the card", ago(90)),
                WireMsg("AIG-2", "Dark mode for the board?", "Lead", "- Dark mode: follow the phone's setting, or always dark?\n\n- I'm assuming **follow the phone**", ago(12), options = listOf("Follow the phone", "Always dark")),
            ),
            open = listOf(OpenCard("AIG-2", "Dark mode for the board?")),
            cards = mapOf("AIG-1" to CardInfo("💬 Phone app", "idea"), "AIG-2" to CardInfo("Dark mode for the board?", "question", waiting = true)),
        ),
        "tunio" to WireThread(
            slug = "tunio", name = "Tunio", emoji = "🎵", color = "#ca8a04", lead = "Lead",
            messages = mutableListOf(
                WireMsg("TUN-7", "💬 Playlist export", "Rich", "Export playlists as M3U please", ago(300), mine = true),
                WireMsg("TUN-7", "💬 Playlist export", "Lead", "- Done: **Export** button on each playlist\n\n- It's in For you to test", ago(240)),
            ),
            cards = mapOf("TUN-7" to CardInfo("💬 Playlist export", "idea")),
        ),
        "_watchman" to WireThread(
            slug = "_watchman", name = "Lead Scrum Master", emoji = "🧭", watchman = true,
            messages = mutableListOf(
                WireMsg("_night", "Tonight", "🧭 Lead Scrum Master", "- Night shift: 3 teams working, nothing stuck", ago(30)),
            ),
        ),
    )
    private val friends = mutableMapOf(
        "davy" to mutableListOf(
            FriendMsg("d1", "in", "Fancy a game later?", ago(50)),
            FriendMsg("d2", "out", "After the barbers, yeah", ago(45)),
        ),
    )

    override suspend fun owner() = "Rich"

    override suspend fun card(slug: String, id: String): CardDetail? {
        val t = threads[slug] ?: return null
        val title = t.cards[id]?.title ?: return null
        return CardDetail(id, title, if (t.open.any { it.id == id }) "team" else "doing", "task",
            "- A demo card from **${t.name}**\n\n- On a real board this shows the card's own description", assignee = t.lead ?: "")
    }

    override suspend fun chats(): List<ChatRef> {
        delay(250)
        val summaries = listOf("ai-gile", "tunio").map { s ->
            val t = threads.getValue(s)
            ChatSummary(t.slug, t.name, t.emoji, t.color, lead = t.lead, unread = t.open.size, last = t.messages.lastOrNull()?.at)
        } + ChatSummary("_watchman", "Lead Scrum Master", "🧭", "#3a5a7a", watchman = true, last = threads.getValue("_watchman").messages.last().at)
        val contacts = friends.map { (id, l) -> Contact(id, "Davy", checked = true, last = l.last().at, lastIn = l.last { it.dir == "in" }.at) }
        return Mapper.chats(summaries, contacts)
    }

    override suspend fun conversation(ref: ChatRef): Conversation {
        delay(150)
        return if (ref.kind == Kind.FRIEND) Mapper.friend(ref, "Rich", friends.getValue(ref.id).toList())
        else Mapper.conversation(ref, threads.getValue(ref.id))
    }

    override suspend fun send(ref: ChatRef, text: String, target: String?): String? {
        delay(300)
        val now = Instant.now().toString()
        if (ref.kind == Kind.FRIEND) {
            friends.getValue(ref.id) += FriendMsg("o$now", "out", text, now)
            return null
        }
        val t = threads.getValue(ref.id)
        val card = target ?: "${t.slug.take(3).uppercase()}-${(100..999).random()}"
        val title = t.cards[card]?.title ?: Mapper.quickTitle(text)
        val msgs = t.messages.toMutableList()
        msgs += WireMsg(card, title, "Rich", text, now, mine = true)
        // The demo lead answers at once, so you can see a reply arrive.
        msgs += WireMsg(card, title, t.lead ?: "🧭 Lead Scrum Master", if (ref.kind == Kind.WATCHMAN) "- Passed on to the AI-gile team" else "- Got it: on it now", Instant.now().plusSeconds(1).toString())
        threads[ref.id] = t.copy(
            messages = msgs,
            open = t.open.filter { it.id != card },
            cards = t.cards + (card to CardInfo(title, "idea")),
        )
        return card
    }

    override suspend fun newMessage(slug: String, text: String) =
        send(ChatRef(Kind.LEAD, slug, slug, "", ""), text, null)
}
