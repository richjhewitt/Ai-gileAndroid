package com.smizflark.aigile.messenger.data

/* ---------- safety code change warning (AII-333) ---------- */

/**
 * The safety words last accepted for each friend, by friend id. The first words seen are taken as they are;
 * other words after that mean the friend's key changed, so sending stops until you have checked the new ones together.
 */
object SafetyWatch {
    const val CHANGED = "Not sent: the safety code changed. Read the new words to each other first."

    /** True when the friend's words are not the ones accepted before. */
    fun changed(known: Map<String, String>, id: String, now: String): Boolean {
        val was = known[id] ?: return false
        return now.isNotBlank() && was != now
    }

    /** Friends seen for the first time get their words remembered; changed ones are left alone. */
    fun learn(known: Map<String, String>, chats: List<ChatRef>): Map<String, String> =
        known + chats.filter { it.kind == Kind.FRIEND && it.safety.isNotBlank() && it.id !in known }.associate { it.id to it.safety }

    /** We read the new words to each other: they are the ones to keep. */
    fun accept(known: Map<String, String>, id: String, now: String): Map<String, String> =
        if (now.isBlank()) known else known + (id to now)
}
