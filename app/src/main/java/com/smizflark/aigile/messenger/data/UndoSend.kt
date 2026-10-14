package com.smizflark.aigile.messenger.data

/** Undo send (AII-202): a message held for [UndoSend.waitMs] after Send. Undo puts [text], [quote] and [files] back in the box. */
data class Held(val text: String, val quote: String?, val to: String?, val files: List<String>) {
    /** What goes out: the quote, a blank line, then the text; a blank text is sent as it is (pictures only). */
    val body: String get() = Quote.with(quote, text.trim()).takeIf { text.isNotBlank() } ?: text
}

object UndoSend {
    /** The choices in Settings (AII-233), in seconds. */
    val CHOICES = listOf(5, 10)

    /** How long to hold a message for a stored setting; anything unknown counts as 5 seconds. */
    fun seconds(setting: Int) = if (setting in CHOICES) setting else 5
    fun waitMs(setting: Int) = seconds(setting) * 1_000L
}
