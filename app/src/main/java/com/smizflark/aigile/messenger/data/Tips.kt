package com.smizflark.aigile.messenger.data

/** Welcome tips (AII-276): three small tips at the top of the chats list, in the demo, until Got it is tapped. */
object Tips {
    fun show(demo: Boolean, seen: Boolean) = demo && !seen
}
