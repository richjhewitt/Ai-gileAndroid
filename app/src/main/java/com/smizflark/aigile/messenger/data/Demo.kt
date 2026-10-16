package com.smizflark.aigile.messenger.data

import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Try the app with no board: a few made-up chats that answer back. Nothing leaves the phone. */
/** AII-86: a made-up photo shipped with the app, so the demo shows a picture with no board. */
private val DEMO_PICTURE = Attachment("android.resource://${com.smizflark.aigile.messenger.BuildConfig.APPLICATION_ID}/drawable/demo_picture", "hills.png", "image/png")
/** AII-91: a second one, so swiping between pictures can be tried. */
private val DEMO_SEA = Attachment("android.resource://${com.smizflark.aigile.messenger.BuildConfig.APPLICATION_ID}/drawable/demo_picture_sea", "sea.png", "image/png")

/** AII-324: the demo friend's safety code. */
const val DEMO_SAFETY = "apple river candle mountain violin harbor"

class DemoSource : ChatSource {
    private fun ago(min: Long) = Now.instant().minus(min, ChronoUnit.MINUTES).toString()

    private val threads = mutableMapOf(
        "ai-gile" to WireThread(
            slug = "ai-gile", name = "AI-gile", emoji = "🗂️", color = "#16a34a", lead = "Lead",
            messages = mutableListOf(
                WireMsg("AIG-1", "💬 Phone app", "Rich", "Can I read my chats on my phone yet?", ago(95), mine = true,
                    attachments = listOf(DEMO_SEA)),
                WireMsg("AIG-1", "💬 Phone app", "Lead", "- Yes, you're looking at it! This is the **Messenger** app\n\n- Type a reply below and I'll see it straight away", ago(90),
                    attachments = listOf(DEMO_PICTURE)),
                WireMsg("AIG-2", "Dark mode for the board?", "Lead", "- Should dark mode follow your phone's setting, or always be dark?\n\n- I'd suggest **follow the phone**", ago(12), options = listOf("Follow the phone", "Always dark")),
            ),
            open = listOf(OpenCard("AIG-2", "Dark mode for the board?")),
            cards = mapOf("AIG-1" to CardInfo("💬 Phone app", "idea"), "AIG-2" to CardInfo("Dark mode for the board?", "question", waiting = true)),
        ),
        "tunio" to WireThread(
            slug = "tunio", name = "Tunio", emoji = "🎵", color = "#ca8a04", lead = "Lead",
            messages = mutableListOf(
                WireMsg("TUN-7", "💬 Playlist export", "Rich", "Can you let me save my playlists as M3U files?", ago(300), mine = true),
                WireMsg("TUN-7", "💬 Playlist export", "Lead", "- Sure! You can try it on your computer first:\n\n```\ntunio export --all --format m3u\n```", ago(260)),   // AII-115: a command box to tap
                WireMsg("TUN-7", "💬 Playlist export", "Lead", "- All done: every playlist now has an **Export** button\n\n- Have a go and tell me what you think", ago(240)),
            ),
            cards = mapOf("TUN-7" to CardInfo("💬 Playlist export", "idea")),
        ),
        "_watchman" to WireThread(
            slug = "_watchman", name = "Lead Scrum Master", emoji = "🧭", watchman = true,
            messages = mutableListOf(
                WireMsg("_night", "Tonight", "🧭 Lead Scrum Master", "- Good evening! 3 of your projects are being worked on and all is going well", ago(30)),
            ),
        ),
    )
    private val friends = mutableMapOf(
        "davy" to mutableListOf(
            FriendMsg("d1", "in", "Fancy a game later?", ago(50), reacts = FriendReacts(theirs = listOf("👍"))),
            FriendMsg("d2", "out", "After the barbers, yeah", ago(45)),
            // AII-331: a voice note from Davy; the file isn't there, so Play says it can't (no sound in the demo).
            FriendMsg("d3", "in", null, ago(40), attachments = listOf(Attachment("file:///demo/voice-1760000000000.m4a", "voice-1760000000000.m4a", Voice.TYPE, 52_000))),
        ),
    )
    /** AII-324: you and Davy have not checked the safety words yet, so the button shows. */
    private var checked = false

    /** The Lead Scrum Master's read mark, which the board keeps (AII-25). */
    private var watchRead = false

    override suspend fun owner() = "Rich"

    /** AII-274: where the demo's test card is; Accept and Send back only change this (AII-273). */
    private val placed = mutableMapOf("TUN-7" to "verify")

    override suspend fun card(slug: String, id: String): CardDetail? {
        val t = threads[slug] ?: return null
        val title = t.cards[id]?.title ?: return null
        val steps = if (id == "TUN-7") "- Open a playlist\n\n- Tap **Export**\n\n- Check the .m3u file opens in your music app" else ""
        return CardDetail(id, title, placed[id] ?: if (t.open.any { it.id == id }) "team" else "doing", "task",
            "- This is a sample task from **${t.name}**\n\n- With your own projects, the details of each task show up here", steps, assignee = t.lead ?: "")
    }

    override suspend fun verdict(slug: String, id: String, accept: Boolean, text: String) {
        if (placed[id] == "verify") placed[id] = if (accept) "done" else "team"
    }

    override suspend fun chats(): List<ChatRef> {
        delay(250)
        val summaries = listOf("ai-gile", "tunio").map { s ->
            val t = threads.getValue(s)
            ChatSummary(t.slug, t.name, t.emoji, t.color, lead = t.lead, unread = t.open.size, last = t.messages.lastOrNull()?.at,
                lastText = t.messages.lastOrNull()?.text, lastMine = t.messages.lastOrNull()?.mine, working = s == "ai-gile", task = "AIG-2 Dark mode for the board?".takeIf { s == "ai-gile" })   // AII-112: one team busy, to see 'working…'
        } + ChatSummary("_watchman", "Lead Scrum Master", "🧭", "#3a5a7a", watchman = true, last = threads.getValue("_watchman").messages.last().at,
            lastText = threads.getValue("_watchman").messages.last().text, lastMine = threads.getValue("_watchman").messages.last().mine,
            unread = if (watchRead) 0 else threads.getValue("_watchman").messages.count { !it.mine })
        val contacts = friends.map { (id, l) -> Contact(id, "Davy", checked = checked, safety = DEMO_SAFETY, last = l.last().at, lastIn = l.last { it.dir == "in" }.at) }
        return Mapper.chats(summaries, contacts)
    }

    override suspend fun conversation(ref: ChatRef): Conversation {
        delay(150)
        if (ref.kind == Kind.WATCHMAN) watchRead = true   // like the board: opening it reads it
        return if (ref.kind == Kind.FRIEND) Mapper.friend(ref, "Rich", friends.getValue(ref.id).toList())
        else Mapper.conversation(ref, threads.getValue(ref.id))
    }

    override suspend fun send(ref: ChatRef, text: String, target: String?, files: List<Upload>, reply: String?): String? {
        delay(300)
        val now = Now.instant().toString()
        // No board here: the demo keeps sent pictures as files on the phone so they still show.
        val att = files.map { f ->
            val out = java.io.File.createTempFile("demo-", "-" + f.name).apply { writeBytes(f.bytes) }
            Attachment(out.toURI().toString(), f.name, f.type, f.bytes.size.toLong())
        }
        if (ref.kind == Kind.FRIEND) {
            val l = friends.getValue(ref.id)
            val quoted = reply?.let { id -> l.firstOrNull { it.id == id } }?.let { FriendReply(it.id, it.text.orEmpty()) }
            l += FriendMsg("o$now-${l.size}", "out", text, now, attachments = att, reply = quoted)
            return null
        }
        val t = threads.getValue(ref.id)
        val card = target ?: "${t.slug.take(3).uppercase()}-${(100..999).random()}"
        val title = t.cards[card]?.title ?: Mapper.quickTitle(text)
        val msgs = t.messages.toMutableList()
        msgs += WireMsg(card, title, "Rich", text, now, mine = true, attachments = att)
        // The demo lead answers at once, so you can see a reply arrive.
        msgs += WireMsg(card, title, t.lead ?: "🧭 Lead Scrum Master", if (ref.kind == Kind.WATCHMAN) "- Thanks, I've passed that on" else "- Got it, I'm on it!", Now.instant().plusSeconds(1).toString())
        threads[ref.id] = t.copy(
            messages = msgs,
            open = t.open.filter { it.id != card },
            cards = t.cards + (card to CardInfo(title, "idea")),
        )
        return card
    }

    override suspend fun edit(ref: ChatRef, id: String, text: String) {
        val l = friends[ref.id] ?: return
        val i = l.indexOfFirst { it.id == id && it.dir == "out" && it.deleted == null }
        if (i >= 0) l[i] = l[i].copy(text = text, edited = Now.instant().toString())
    }

    override suspend fun delete(ref: ChatRef, id: String) {
        val l = friends[ref.id] ?: return
        val i = l.indexOfFirst { it.id == id && it.dir == "out" }
        if (i >= 0) l[i] = l[i].copy(text = "", attachments = emptyList(), deleted = Now.instant().toString())
    }

    override suspend fun react(ref: ChatRef, id: String, emoji: String) {
        val l = friends[ref.id] ?: return
        val i = l.indexOfFirst { it.id == id && it.deleted == null }
        if (i < 0) return
        val r = l[i].reacts ?: FriendReacts()
        l[i] = l[i].copy(reacts = r.copy(mine = if (emoji in r.mine) r.mine - emoji else r.mine + emoji))
    }

    override suspend fun setChecked(ref: ChatRef) { checked = true }

    override suspend fun newMessage(slug: String, text: String, files: List<Upload>) =
        send(ChatRef(Kind.LEAD, slug, slug, "", ""), text, null, files)
}
