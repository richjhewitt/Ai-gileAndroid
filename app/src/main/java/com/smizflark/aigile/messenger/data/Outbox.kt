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

    /** Failed for good: the server refused, or the tries ran out. These get the red mark. */
    fun failedForGood(l: List<Pending>) = l.filter { it.failed && !it.offline }

    /** Failed for good between [before] and [after] (AII-151): these get a Not sent alert, once. */
    fun newlyFailed(before: List<Pending>, after: List<Pending>): List<Pending> {
        val had = failedForGood(before).map { it.id }.toSet()
        return failedForGood(after).filter { it.id !in had }
    }
}
