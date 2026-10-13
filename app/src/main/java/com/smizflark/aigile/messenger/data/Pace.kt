package com.smizflark.aigile.messenger.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

/**
 * How often a screen looks for new messages (AII-26): quick while a chat is busy, then slower and slower
 * while it is quiet, so an open chat costs little battery. [poke] wakes it at once (you just sent something).
 */
class Pace(private val fast: Long, private val slow: Long) {
    var wait = fast
        private set
    private val pokes = Channel<Unit>(Channel.CONFLATED)

    /** The wait before the next look: back to quick when something changed, else half as long again, up to [slow]. */
    fun after(changed: Boolean): Long {
        wait = if (changed) fast else minOf(slow, wait * 3 / 2)
        return wait
    }

    /** Look again now, and stay quick: a reply is likely soon. */
    fun poke() { wait = fast; pokes.trySend(Unit) }

    /** Waits [wait], or less if poked. */
    suspend fun sleep() { withTimeoutOrNull(wait) { pokes.receive() } }
}
