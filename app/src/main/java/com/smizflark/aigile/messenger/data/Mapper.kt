package com.smizflark.aigile.messenger.data

/** Turns the board's answers into what the screens show. Pure, so it is unit tested. */
object Mapper {

    /** The card id at the start of a roster task ("AII-5 Title" -> "AII-5"), or null (AII-113). */
    fun cardId(task: String): String? = CARD_ID.find(task)?.groupValues?.get(1)
    private val CARD_ID = Regex("""^\s*([A-Z][A-Z0-9]*-\d+)\b""")

    /** Leads first (the board's own order), then friends, newest first; the 🧭 Lead Scrum Master on top when it has news. */
    fun chats(summaries: List<ChatSummary>, contacts: List<Contact>): List<ChatRef> {
        val leads = summaries.map { s ->
            if (s.watchman) ChatRef(
                kind = Kind.WATCHMAN, id = "_watchman", name = "Lead Scrum Master",
                subtitle = "Passes your message to the right team", emoji = s.emoji ?: "🧭",
                color = s.color, last = s.last, unread = s.unread,
                preview = s.lastText?.let { plainLine(it) }?.takeIf { it.isNotEmpty() }, previewMine = s.lastMine == true,   // AII-108
            ) else ChatRef(
                kind = Kind.LEAD, id = s.slug, name = s.name.ifBlank { s.slug },
                subtitle = s.lead ?: "Lead", emoji = s.emoji.orEmpty(), color = s.color,
                pic = s.leadPic?.takeIf { it.isNotBlank() } ?: s.logo?.takeIf { it.isNotBlank() },
                waiting = s.unread, last = s.last, seen = s.seen?.takeIf { it.isNotBlank() },
                preview = s.lastText?.let { plainLine(it) }?.takeIf { it.isNotEmpty() }, previewMine = s.lastMine == true,
                working = s.working == true,   // AII-112
                workingOn = s.task?.let { cardId(it) }?.takeIf { s.working == true },   // AII-113
            )
        }
        val friends = contacts.filter { !it.blocked }.sortedByDescending { it.last ?: "" }.map { c ->
            ChatRef(
                kind = Kind.FRIEND, id = c.id, name = c.name.ifBlank { "Friend" },
                subtitle = if (c.checked) "🔒 Private, safety code checked" else "🔒 Private",
                emoji = "", last = c.last, lastIn = c.lastIn,
            )
        }
        return leads + friends
    }

    /** AII-106: an older board doesn't say who wrote the newest message; the chat as last fetched does, if it ends with the same one. */
    fun mineFromCache(ref: ChatRef, cached: Conversation?): ChatRef {
        if (ref.preview == null || ref.previewMine) return ref
        val b = cached?.bubbles?.lastOrNull { it.state == SendState.SENT } ?: return ref
        val text = plainLine(b.text)
        val same = b.at == ref.last || text == ref.preview || (ref.preview.length >= 100 && text.startsWith(ref.preview))   // the board cuts it at 120
        return if (b.mine && same) ref.copy(previewMine = true) else ref
    }

    /** A message as one plain line for the chats list: no bullets, bold or code marks ("- Done: **Export**" -> "Done: Export"). */
    fun plainLine(text: String): String = text.replace(Regex("\\s+"), " ").trim()
        .replace(Regex("^(?:[-*•]|\\d+[.)])\\s+"), "").replace(Regex("\\*\\*|__|`+|^#+\\s*"), "")

    /** AII-64: picture fallback when a chat has no picture or emoji: "Tunio Discord" -> "TD", "rich" -> "R". */
    fun initials(name: String): String {
        val words = name.split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotEmpty() }
        return words.take(2).joinToString("") { it.substring(0, it.offsetByCodePoints(0, 1)) }.uppercase().ifEmpty { "?" }
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
                pics = pics("/api/projects/${t.slug}/attachments/", m.attachments),
            )
        }.filter { it.text.isNotEmpty() || it.pics.isNotEmpty() }
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
                pics = if (m.verified) pics("/api/dms/contacts/${ref.id}/files/", m.attachments) else emptyList(),
            )
        }.filter { it.text.isNotEmpty() || it.pics.isNotEmpty() }
        return Conversation(ref, bubbles, lastFromThem = bubbles.lastOrNull { !it.mine }?.at)
    }

    fun mimeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"; "png" -> "image/png"; "gif" -> "image/gif"; "webp" -> "image/webp"
        "heic" -> "image/heic"; "bmp" -> "image/bmp"; "pdf" -> "application/pdf"; "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    /** A message's files as paths on the board (the demo keeps file:// addresses as they are). */
    fun pics(base: String, files: List<Attachment>) = files.filter { it.id.isNotBlank() }.map {
        Pic(if (it.id.startsWith("file:") || it.id.startsWith("android.resource:")) it.id else base + it.id.replace(" ", "%20"), it.name, it.type)
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

    /** The phone key as Settings shows it: only the last 4 characters. */
    fun maskKey(key: String): String = when {
        key.isEmpty() -> "none"
        key.length <= 4 -> "••••"
        else -> "••••" + key.takeLast(4)
    }

    /** Did a change in Settings move the phone to another board (so the old board's cache must go)? */
    fun boardChanged(wasDemo: Boolean, oldUrl: String, newUrl: String) = wasDemo || oldUrl != newUrl

    /**
     * The board's pairing QR -> url to credential. aigile://pair?url=&id=&key= (sealed, AII-19) gives "id:key";
     * the older aigile://pair?url=&key= gives the key alone.
     */
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
        return normaliseUrl(url) to credential(params["id"], key)
    }

    /** One string for the key box: "id:key" when the board gave a device id. */
    fun credential(id: String?, key: String): String = if (id.isNullOrBlank()) key else "${id.trim()}:$key"

    /** "id:key" -> device id to key; a plain key -> "" to key. Device ids are hex, keys base64url, so ':' never clashes. */
    fun splitCredential(text: String): Pair<String, String> {
        val t = text.trim()
        val id = t.substringBefore(':', "")
        return if (id.isNotEmpty() && Regex("^[0-9A-Fa-f]+$").matches(id)) id to t.substringAfter(':') else "" to t
    }
}
