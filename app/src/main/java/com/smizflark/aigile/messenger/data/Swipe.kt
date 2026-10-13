package com.smizflark.aigile.messenger.data

/** What a swipe on a chat row does (AII-61). */
enum class SwipeAction { READ, MUTE, UNMUTE }

/** Right = mark read, left = mute (or unmute when the chat is already muted). Other directions do nothing. */
fun swipeAction(toEnd: Boolean?, muted: Boolean): SwipeAction? = when (toEnd) {
    true -> SwipeAction.READ
    false -> if (muted) SwipeAction.UNMUTE else SwipeAction.MUTE
    null -> null
}
