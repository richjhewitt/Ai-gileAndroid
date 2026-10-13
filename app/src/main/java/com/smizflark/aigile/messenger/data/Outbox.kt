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

    /** Failed for good: the server refused, or the tries ran out. These get the red mark. */
    fun failedForGood(l: List<Pending>) = l.filter { it.failed && !it.offline }
}
