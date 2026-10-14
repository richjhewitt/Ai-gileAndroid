package com.smizflark.aigile.messenger.data

/** State changes of the outbox list (AII-62): a message that failed for good can be retried or deleted. */
object Outbox {
    /** A send went wrong: keep the message, marked failed. [offline]: it only lacked a connection. */
    fun fail(l: List<Pending>, id: String, offline: Boolean) =
        l.map { if (it.id == id) it.copy(failed = true, offline = offline) else it }

    /** Tap Retry: it is a fresh try, no longer marked failed. */
    fun retry(l: List<Pending>, id: String) =
        l.map { if (it.id == id) it.copy(failed = false, offline = false) else it }

    /** Sent, or Delete: gone from the outbox. */
    fun remove(l: List<Pending>, id: String) = l.filter { it.id != id }

    /** Edit a Send later message (AII-150): its words go back in the box, above anything already typed. */
    fun editBack(p: Pending, typed: String) = if (typed.isBlank()) p.text else p.text + "\n" + typed

    /** Change time on a Send later message (AII-169): it waits for [at] instead. */
    fun moveTo(l: List<Pending>, id: String, at: java.time.Instant) =
        l.map { if (it.id == id) it.copy(sendAt = at.toString()) else it }

    /** Repeat a Send later message every day or week (AII-174); null stops it repeating. */
    fun setRepeat(l: List<Pending>, id: String, repeat: String?) =
        l.map { if (it.id == id) it.copy(repeat = repeat) else it }

    /** The next one of a repeating message (AII-174): same words, a day or a week on from its time, the first such time after [now]. Pictures don't repeat. */
    fun nextRepeat(p: Pending, now: java.time.Instant, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Pending? {
        val days = when (p.repeat) { "day" -> 1L; "week" -> 7L; else -> return null }
        var t = (p.sendAt?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() } ?: now).atZone(zone)
        do t = t.plusDays(days) while (!t.toInstant().isAfter(now))
        return p.copy(id = java.util.UUID.randomUUID().toString(), at = now.toString(), failed = false, offline = false, files = emptyList(), sendAt = t.toInstant().toString())
    }

    /** Every Send later message still waiting, in every chat, soonest first (AII-166); messages of chats no longer listed are left out. */
    fun waiting(l: List<Pending>, chats: List<ChatRef>, now: java.time.Instant): List<Pair<ChatRef, Pending>> {
        val byKey = chats.associateBy { it.key }
        return l.filter { com.smizflark.aigile.messenger.work.later(it, now) }
            .mapNotNull { p -> byKey[p.chat]?.let { it to p } }.sortedBy { java.time.Instant.parse(it.second.sendAt) }
    }

    /** Send later messages that went between [before] and [after] (AII-175): these get a Sent alert. Send now clears the time first, so those don't. */
    fun sentLater(before: List<Pending>, after: List<Pending>): List<Pending> {
        val left = after.map { it.id }.toSet()
        return before.filter { it.sendAt != null && it.id !in left }
    }

    /** Failed for good: the server refused, or the tries ran out. These get the red mark. */
    fun failedForGood(l: List<Pending>) = l.filter { it.failed && !it.offline }

    /** Failed for good between [before] and [after] (AII-151): these get a Not sent alert, once. */
    fun newlyFailed(before: List<Pending>, after: List<Pending>): List<Pending> {
        val had = failedForGood(before).map { it.id }.toSet()
        return failedForGood(after).filter { it.id !in had }
    }
}
