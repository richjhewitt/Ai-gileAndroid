package com.smizflark.aigile.messenger.data

/** Silent alerts for one chat (AII-207): its alerts still show, with no sound and no buzz. Mute (nothing shown) wins over this. */
object Silent {
    /** Switches one chat on or off. */
    fun toggle(silent: Set<String>, key: String): Set<String> = if (key in silent) silent - key else silent + key

    /** Whether the chat's alert is shown without sound: silent and not muted (a muted chat shows nothing at all). */
    fun quiet(silent: Set<String>, muted: Set<String>, key: String) = key in silent && key !in muted
}
