package com.smizflark.aigile.messenger.data

/** What a swipe on a chat row does (AII-61, AII-216). */
enum class SwipeAction { READ, MUTE, UNMUTE, PIN, CLOSE }

/** What a swipe can be set to in Settings (AII-216); [id] is what the Store keeps. */
enum class SwipeChoice(val id: String) {
    READ("read"), MUTE("mute"), PIN("pin"), CLOSE("close"), NOTHING("nothing");

    companion object { fun of(id: String?, default: SwipeChoice) = entries.firstOrNull { it.id == id } ?: default }
}

/** Starting choices: right = mark read, left = mute. */
val SWIPE_RIGHT_DEFAULT = SwipeChoice.READ
val SWIPE_LEFT_DEFAULT = SwipeChoice.MUTE

/** The chosen action for a swipe; mute turns into unmute when the chat is already muted. Other directions and Nothing do nothing. */
fun swipeAction(toEnd: Boolean?, muted: Boolean, right: SwipeChoice = SWIPE_RIGHT_DEFAULT, left: SwipeChoice = SWIPE_LEFT_DEFAULT): SwipeAction? =
    when (when (toEnd) { true -> right; false -> left; null -> SwipeChoice.NOTHING }) {
        SwipeChoice.READ -> SwipeAction.READ
        SwipeChoice.MUTE -> if (muted) SwipeAction.UNMUTE else SwipeAction.MUTE
        SwipeChoice.PIN -> SwipeAction.PIN
        SwipeChoice.CLOSE -> SwipeAction.CLOSE
        SwipeChoice.NOTHING -> null
    }
