package com.smizflark.aigile.messenger.data

/** Turns the board's answers into what the screens show. Pure, so it is unit tested. */
object Mapper {

    /** Leads first (the board's own order), then friends, newest first; the 🧭 Lead Scrum Master on top when it has news. */
    fun chats(summaries: List<ChatSummary>, contacts: List<Contact>): List<ChatRef> {
        val leads = summaries.map { s ->
            if (s.watchman) ChatRef(
                kind = Kind.WATCHMAN, id = "_watchman", name = "Lead Scrum Master",
                subtitle = "Passes your message to the right team", emoji = s.emoji ?: "🧭",
                color = s.color, last = s.last, unread = s.unread,
            ) else ChatRef(
                kind = Kind.LEAD, id = s.slug, name = s.name.ifBlank { s.slug },
                subtitle = s.lead ?: "Lead", emoji = s.emoji ?: "🗂️", color = s.color,
                pic = s.leadPic?.takeIf { it.isNotBlank() } ?: s.logo?.takeIf { it.isNotBlank() },
                waiting = s.unread, last = s.last, seen = s.seen?.takeIf { it.isNotBlank() },
            )
        }
        val friends = contacts.filter { !it.blocked }.sortedByDescending { it.last ?: "" }.map { c ->
            ChatRef(
                kind = Kind.FRIEND, id = c.id, name = c.name.ifBlank { "Friend" },
                subtitle = if (c.checked) "🔒 Private, safety code checked" else "🔒 Private",
                emoji = "🔒", last = c.last, lastIn = c.lastIn,
            )
        }
        return leads + friends
    }

    /** A lead's chat (or the Lead Scrum Master's) as bubbles, plus where a reply can go. */
    fun conversation(ref: ChatRef, t: WireThread): Conversation {
        val waitingIds = t.open.map { it.id }.toSet()
        val bubbles = t.messages.mapIndexed { i, m ->
            Bubble(
                key = "${m.card}:${m.at}:$i",
                author = m.author,
                text = (m.text ?: "").trim(),
                at = m.at,
                mine = m.mine,
                card = m.card?.takeIf { it != "_night" },
                cardTitle = m.title ?: t.cards[m.card]?.title,
                options = m.options,
                waiting = m.card in waitingIds,
            )
        }.filter { it.text.isNotEmpty() }
        val lastFromThem = bubbles.lastOrNull { !it.mine }?.at
        if (ref.kind == Kind.WATCHMAN) return Conversation(ref, bubbles, lastFromThem = lastFromThem)

        val targets = mutableListOf<Target>()
        t.open.forEach { targets += Target(it.id, "${it.id} ${clean(it.title)}", waiting = true) }
        // Recent cards in this chat that are still going (newest first), so a reply can carry on there.
        bubbles.asReversed().mapNotNull { it.card }.distinct()
            .filter { id -> id !in waitingIds && t.cards[id]?.done != true && t.cards[id]?.kind != "test" }
            .take(4)
            .forEach { id -> targets += Target(id, "$id ${clean(t.cards[id]?.title ?: "")}") }
        targets += Target(null, "New message")

        val lastCard = bubbles.lastOrNull()?.card
        val default = t.open.firstOrNull()?.id
            ?: lastCard?.takeIf { id -> targets.any { it.id == id } }
        return Conversation(ref, bubbles, targets, default, lastFromThem)
    }

    fun friend(ref: ChatRef, owner: String, msgs: List<FriendMsg>): Conversation {
        val bubbles = msgs.mapIndexed { i, m ->
            val mine = m.dir == "out"
            Bubble(
                key = m.id ?: "$i:${m.at}",
                author = if (mine) owner else ref.name,
                text = if (m.verified) (m.text ?: "") else "⚠️ Not verified: this message could not be checked",
                at = m.at, mine = mine, unverified = !m.verified,
            )
        }.filter { it.text.isNotEmpty() }
        return Conversation(ref, bubbles, lastFromThem = bubbles.lastOrNull { !it.mine }?.at)
    }

    /** "💬 can you check..." -> "can you check...": card titles read better without the leading emoji. */
    fun clean(title: String) = title.replace(Regex("^[💬📣🧹📱\\s]+"), "").take(60)

    /** The quick-card title the board's own page uses for a new 💬 message. */
    fun quickTitle(text: String) = "💬 " + text.lineSequence().first().take(80)

    /** Board address as typed -> https://host[:port], no trailing slash. */
    fun normaliseUrl(raw: String): String {
        var u = raw.trim().trimEnd('/')
        if (u.isEmpty()) return u
        if (!Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(u)) u = "https://$u"
        return u
    }

    /** Plain http to anything but a Tailscale address sends the key in the clear. */
    fun insecure(url: String): Boolean {
        if (!url.startsWith("http://", ignoreCase = true)) return false
        val host = url.removePrefix("http://").substringBefore('/').substringBefore(':').lowercase()
        val tailscale = host.endsWith(".ts.net") || Regex("^100\\.(6[4-9]|[7-9]\\d|1[01]\\d|12[0-7])\\.").containsMatchIn(host)
        return !tailscale && host != "localhost" && host != "127.0.0.1" && host != "10.0.2.2"
    }

    /** aigile://pair?url=...&key=... (the board's pairing QR) -> url to key. */
    fun parsePairing(text: String): Pair<String, String>? {
        val t = text.trim()
        if (!t.startsWith("aigile://pair", ignoreCase = true)) return null
        val query = t.substringAfter('?', "")
        val params = query.split('&').mapNotNull {
            val k = it.substringBefore('=', ""); if (k.isEmpty()) null
            else k to java.net.URLDecoder.decode(it.substringAfter('='), "UTF-8")
        }.toMap()
        val url = params["url"] ?: return null
        val key = params["key"] ?: params["token"] ?: return null
        return normaliseUrl(url) to key
    }
}
