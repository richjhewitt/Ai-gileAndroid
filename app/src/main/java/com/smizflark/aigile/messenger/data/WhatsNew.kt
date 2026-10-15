package com.smizflark.aigile.messenger.data

/** What's new bar (AII-268): shown on the first open after the app's version code changes. */
object WhatsNew {
    /** [seen] is the version stored last time (0 = none yet: a brand-new install, so no bar). */
    fun show(seen: Int, current: Int) = seen != 0 && current != seen
}
