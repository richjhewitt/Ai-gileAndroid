package com.smizflark.aigile.messenger.data

/** AII-370: a GIF or sticker pasted or sent from the keyboard. Which of it goes in the message, and where. */
object PasteContent {
    enum class Verdict { ADD, NOT_IN_TEAM, IGNORE }

    /** Every picture type counts, GIF and WebP included (stickers come as image/png or image/webp). */
    fun isPicture(mime: String?): Boolean = mime?.trim()?.lowercase()?.startsWith("image/") == true

    /** Friend chats take the pictures; team chats say so and ignore; anything that is no picture is left to the text box. */
    fun verdict(kind: Kind, mimes: List<String?>): Verdict = when {
        mimes.none { isPicture(it) } -> Verdict.IGNORE
        kind == Kind.FRIEND -> Verdict.ADD
        else -> Verdict.NOT_IN_TEAM
    }
}
