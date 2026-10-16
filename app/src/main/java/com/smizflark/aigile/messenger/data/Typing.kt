package com.smizflark.aigile.messenger.data

/** AII-390: tells the friend I am typing, but only when Settings > Privacy > Show when I'm typing is on. The one place that guards it. */
suspend fun sendTyping(on: Boolean, source: ChatSource, ref: ChatRef) {
    if (on) source.typing(ref)
}
