package com.smizflark.aigile.messenger.data

/** What a hardware-keyboard shortcut does (AII-408). */
enum class KeyAction { SEARCH, NEW_MESSAGE, BACK, NEXT_CHAT, PREVIOUS_CHAT }

/** Android key codes used here (kept as numbers so the logic stays plain Kotlin and unit tested). */
object Keys {
    const val ESCAPE = 111
    const val DPAD_UP = 19
    const val DPAD_DOWN = 20
    const val F = 34
    const val N = 42
}

/** The action for a key press: Ctrl+F search, Ctrl+N new message, Esc back, Alt+Down / Alt+Up next / previous chat. Null for anything else. */
fun keyAction(keyCode: Int, ctrl: Boolean, alt: Boolean, shift: Boolean = false, meta: Boolean = false): KeyAction? = when {
    keyCode == Keys.ESCAPE && !ctrl && !alt && !shift && !meta -> KeyAction.BACK
    keyCode == Keys.F && ctrl && !alt && !shift -> KeyAction.SEARCH
    keyCode == Keys.N && ctrl && !alt && !shift -> KeyAction.NEW_MESSAGE
    keyCode == Keys.DPAD_DOWN && alt && !ctrl && !shift -> KeyAction.NEXT_CHAT
    keyCode == Keys.DPAD_UP && alt && !ctrl && !shift -> KeyAction.PREVIOUS_CHAT
    else -> null
}

/** The chat key [delta] (+1 next, -1 previous) steps from [current] in [keys] (chats-list order). No current chat: next is the first, previous the last. Null at either end or with no chats. */
fun neighbourChat(keys: List<String>, current: String?, delta: Int): String? {
    if (keys.isEmpty()) return null
    val i = keys.indexOf(current ?: "")
    if (i < 0) return if (delta > 0) keys.first() else keys.last()
    return keys.getOrNull(i + delta)
}
