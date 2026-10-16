package com.smizflark.aigile.messenger.data

/** Replying from the notification (AII-24). Pure, so it is unit tested; the receiver is work/ReplyReceiver. */
object QuickReply {
    /** Up to this many chats get their own notification with a Reply button; more are folded into one line. */
    const val MAX_SINGLE = 5

    /** Typed text worth sending, or null for an empty reply. */
    fun clean(text: CharSequence?): String? = text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    /** Same place the chat screen replies by default: the question waiting on you, else a new 💬 message. */
    fun target(convo: Conversation?): String? = convo?.defaultTarget

    /** The newest message from them, shortened for the notification. */
    fun preview(convo: Conversation?, max: Int = 160): String? {
        val b = convo?.bubbles?.lastOrNull { !it.mine } ?: return null
        val t = b.text.replace(Regex("\\s+"), " ").trim()
        if (t.isEmpty()) return null
        return if (t.length <= max) t else t.take(max - 1).trimEnd() + "…"
    }

    /** One notification per chat, so a reply lands in the right one. Stable for the same chat; never the summary's id. */
    fun notificationId(chat: String): Int = (chat.hashCode() and 0x3fffffff) + 100

    /** The group summary's id; chat notifications are all above it. */
    const val SUMMARY_ID = 1

    /** Their newest messages after `since`, oldest first so the newest sits last, each shortened. */
    fun recentLines(convo: Conversation?, since: String?, max: Int = 5, each: Int = 120): List<String> =
        convo?.bubbles.orEmpty().filter { !it.mine && it.at > (since ?: "") }
            .map { cut(it.text, each) }.filter { it.isNotEmpty() }.takeLast(max)

    /** Whitespace tidied and cut to `max` characters with an ellipsis. */
    fun cut(text: String, max: Int): String {
        val t = text.replace(Regex("\\s+"), " ").trim()
        return if (t.length <= max) t else t.take(max - 1).trimEnd() + "…"
    }

    /** With no chat notifications left (only the summary, or nothing), the summary should go too. */
    fun summaryIsLonely(activeIds: Collection<Int>): Boolean = activeIds.none { it != SUMMARY_ID }

    /** What the notification says once the reply has gone, or failed. */
    fun outcome(sent: Boolean, error: String?): String =
        if (sent) "Sent ✓" else "Not sent: ${error ?: "no connection"}. Open the chat to try again."
}
