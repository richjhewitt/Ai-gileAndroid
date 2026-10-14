package com.smizflark.aigile.messenger.data

/** Undo send (AII-202): a message held for [UndoSend.WAIT_MS] after Send. Undo puts [text], [quote] and [files] back in the box. */
data class Held(val text: String, val quote: String?, val to: String?, val files: List<String>) {
    /** What goes out: the quote, a blank line, then the text; a blank text is sent as it is (pictures only). */
    val body: String get() = Quote.with(quote, text.trim()).takeIf { text.isNotBlank() } ?: text
}

object UndoSend {
    const val WAIT_MS = 5_000L
}
