package com.smizflark.aigile.messenger.data

/** Text shared in from another app (Android's Share button). Pure, so it is unit tested. */
object Share {
    /** Subject and text as one message: the subject on top unless the text already starts with it. Null if nothing to send. */
    fun text(subject: String?, text: String?): String? {
        val s = subject?.trim().orEmpty()
        val t = text?.trim().orEmpty()
        val out = when {
            s.isEmpty() -> t
            t.isEmpty() -> s
            t.startsWith(s) -> t
            else -> "$s\n\n$t"
        }
        return out.ifEmpty { null }
    }
}
