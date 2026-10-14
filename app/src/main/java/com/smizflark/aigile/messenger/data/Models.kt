package com.smizflark.aigile.messenger.data

import kotlinx.serialization.Serializable

/* ---------- wire: what the board's API returns (unknown fields are ignored) ---------- */

@Serializable
data class Meta(val owner: String = "Owner", val sealed: Int = 0)

/** One row of GET /api/chats: a project's lead, or the 🧭 Lead Scrum Master (watchman = true). */
@Serializable
data class ChatSummary(
    val slug: String,
    val name: String = "",
    val emoji: String? = null,
    val color: String? = null,
    val logo: String? = null,
    val lead: String? = null,
    val leadPic: String? = null,
    val unread: Int = 0,
    val last: String? = null,
    val watchman: Boolean = false,
    /** The board's read mark for a lead chat (AII-44): newest message time read, on the PC or a phone. */
    val seen: String? = null,
    /** The newest message, on one line (the board's preview, AIG-917). */
    val lastText: String? = null,
    /** That message is yours, when the board says (older boards don't: the app's own copy of the chat tells, AII-106). */
    val lastMine: Boolean? = null,
    /** The team is at work right now (AII-111; older boards don't say). */
    val working: Boolean? = null,
    /** What the team at work is on, like "AII-5 Title" (AII-113). */
    val task: String? = null,
)

@Serializable
data class WireMsg(
    val card: String? = null,
    val title: String? = null,
    val author: String = "",
    val text: String? = null,
    val at: String = "",
    val mine: Boolean = false,
    val options: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
)

/** A file on a message: board comments give id, name, type and size; friend DMs give id, name and type. */
@Serializable
data class Attachment(val id: String = "", val name: String = "", val type: String = "", val size: Long = 0)

@Serializable
data class OpenCard(val id: String, val title: String = "")

@Serializable
data class CardInfo(
    val title: String? = null,
    val kind: String? = null,
    val waiting: Boolean = false,
    val done: Boolean = false,
)

/** GET /api/chats/:slug (and /api/chats/_watchman). */
@Serializable
data class WireThread(
    val slug: String,
    val name: String = "",
    val emoji: String? = null,
    val color: String? = null,
    val lead: String? = null,
    val leadPic: String? = null,
    val messages: List<WireMsg> = emptyList(),
    val open: List<OpenCard> = emptyList(),
    val cards: Map<String, CardInfo> = emptyMap(),
    val watchman: Boolean = false,
    /** Starred messages on the board (AII-141), as "fold|at"; null from an older board. */
    val stars: List<String>? = null,
)

/** A friend from GET /api/dms/contacts (private DMs, sealed by the board). */
@Serializable
data class Contact(
    val id: String,
    val name: String = "",
    val blocked: Boolean = false,
    val checked: Boolean = false,
    val last: String? = null,
    val lastIn: String? = null,
)

@Serializable
data class FriendMsg(
    val id: String? = null,
    val dir: String = "in",
    val text: String? = null,
    val at: String = "",
    val verified: Boolean = true,
    val attachments: List<Attachment> = emptyList(),
)

@Serializable
data class Created(val id: String? = null)

/** GET /api/projects/<slug>/cards/<id>: one card for the card view (AII-45). */
@Serializable
data class CardDetail(
    val id: String,
    val title: String = "",
    val status: String = "",
    val type: String = "",
    val description: String = "",
    val testSteps: String = "",
    val assignee: String = "",
)

/* ---------- app: what the screens show ---------- */

enum class Kind { LEAD, WATCHMAN, FRIEND }

@Serializable
data class ChatRef(
    val kind: Kind,
    val id: String,
    val name: String,
    val subtitle: String,
    val emoji: String,
    val color: String? = null,
    val pic: String? = null,
    /** Questions waiting on you (leads only). */
    val waiting: Int = 0,
    /** Newest message time, ISO. */
    val last: String? = null,
    /** Newest message from them, ISO, when the board says (friends). */
    val lastIn: String? = null,
    /** Unread messages as the board counts them (the Lead Scrum Master): read on the PC = read here. */
    val unread: Int = 0,
    /** The board's read mark (leads, AII-44). */
    val seen: String? = null,
    /** The newest message on one line, shown under the name instead of [subtitle] (AII-106). */
    val preview: String? = null,
    /** [preview] is your own message: the row puts "You:" in front. */
    val previewMine: Boolean = false,
    /** The team is at work right now: "working…" under the name (AII-112). */
    val working: Boolean = false,
    /** The card the team at work is on, like "AII-5" (AII-113). */
    val workingOn: String? = null,
) {
    val key get() = "${kind.name.lowercase()}:$id"
}

/** LATER: held back by Send later (AII-146) until its time. */
enum class SendState { SENT, SENDING, FAILED, LATER }

@Serializable
data class Bubble(
    val key: String,
    val author: String,
    val text: String,
    val at: String,
    val mine: Boolean,
    val card: String? = null,
    val cardTitle: String? = null,
    val options: List<String> = emptyList(),
    val state: SendState = SendState.SENT,
    val unverified: Boolean = false,
    /** This card is still waiting on you (an open question). */
    val waiting: Boolean = false,
    val pics: List<Pic> = emptyList(),
)

/** A picture or file on a message: a board path (/api/...) or, before it is sent or in the demo, a file:// address. */
@Serializable
data class Pic(val path: String, val name: String = "", val type: String = "") {
    val local get() = path.startsWith("file:") || path.startsWith("content:") || path.startsWith("android.resource:")
    val image get() = type.startsWith("image/") || Regex("(?i)\\.(png|jpe?g|gif|webp|bmp|heic)$").containsMatchIn(name.ifEmpty { path })
}

/** Where a reply goes: an existing card, or (id = null) a new 💬 message. */
@Serializable
data class Target(val id: String?, val label: String, val waiting: Boolean = false)

@Serializable
data class Conversation(
    val ref: ChatRef,
    val bubbles: List<Bubble>,
    val targets: List<Target> = emptyList(),
    val defaultTarget: String? = null,
    /** The newest message from someone else, for "seen" and alerts. */
    val lastFromThem: String? = null,
    /** The board's stars for this chat (AII-141), as "fold|at"; null when the board doesn't keep them. */
    val boardStars: List<String>? = null,
)

/** A message typed on the phone that hasn't reached the board yet. */
@Serializable
data class Pending(
    val id: String,
    val chat: String,
    val text: String,
    val target: String?,
    val at: String,
    val failed: Boolean = false,
    /** Pictures waiting to go with it, copied into the app so nothing is lost offline. */
    val files: List<String> = emptyList(),
    /** Failed only for want of a connection: it goes by itself when the phone is back online (AII-55). */
    val offline: Boolean = false,
    /** Send later (AII-146): it waits until this time (ISO instant); null sends at once. */
    val sendAt: String? = null,
)
