package com.smizflark.aigile.messenger.data

/**
 * A chat's own name on this phone (AII-365): shown everywhere the chat's name shows; the board never hears it.
 * Stored per chat in the settings prefs (backed up) as "key<TAB>name"; no entry means the board's name.
 * [apply] is the one place it is put on a [ChatRef] (Repo.chats and Store.cachedChats); [ChatRef.boardName] keeps the board's name for Reset.
 */
object ChatNames {
    const val MAX = 40
    private const val SEP = '\t'

    /** What Save stores: whitespace squeezed, trimmed, at most [MAX] characters; "" means Reset. */
    fun clean(input: String): String = input.replace(Regex("\\s+"), " ").trim().take(MAX).trim()

    fun of(saved: Set<String>, key: String): String? =
        saved.firstOrNull { it.substringBefore(SEP) == key }?.substringAfter(SEP, "")?.takeIf { it.isNotBlank() }

    /** Saves [name] for [key]; blank removes the entry (Reset). */
    fun put(saved: Set<String>, key: String, name: String): Set<String> {
        val n = clean(name)
        return saved.filterNot { it.substringBefore(SEP) == key }.toSet() + (if (n.isEmpty()) emptySet() else setOf("$key$SEP$n"))
    }

    /** The board's name for a chat, renamed or not. */
    fun boardName(ref: ChatRef): String = ref.boardName ?: ref.name

    /** [ref] with its local name, or with the board's name back when there is none; idempotent. */
    fun on(ref: ChatRef, saved: Set<String>): ChatRef {
        val board = boardName(ref)
        val own = of(saved, ref.key)?.takeIf { it != board }
        return if (own == null) ref.copy(name = board, boardName = null) else ref.copy(name = own, boardName = board)
    }

    fun apply(chats: List<ChatRef>, saved: Set<String>): List<ChatRef> =
        if (saved.isEmpty() && chats.none { it.boardName != null }) chats else chats.map { on(it, saved) }
}
