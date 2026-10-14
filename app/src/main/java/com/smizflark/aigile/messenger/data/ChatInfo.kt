package com.smizflark.aigile.messenger.data

/** What the chat details view lists (AII-80): the cards still open and the pictures sent in the chat. */
data class ChatInfo(val openCards: List<Target>, val pictures: List<Pic>, val links: List<ChatLink> = emptyList(), val files: List<Pic> = emptyList())

/** Open cards are the ones waiting on you; pictures are newest first, each only once. */
fun chatInfo(c: Conversation?): ChatInfo {
    if (c == null) return ChatInfo(emptyList(), emptyList())
    val open = c.targets.filter { it.id != null && it.waiting }
    val pics = c.bubbles.asReversed().flatMap { it.pics }.filter { it.image }.distinctBy { it.path }
    return ChatInfo(open, pics, Links.of(c.bubbles.map { it.text }), files(c))
}

/** AII-197: every file that is not a picture, newest first, each once. */
fun files(c: Conversation): List<Pic> = c.bubbles.asReversed().flatMap { it.pics }.filter { !it.image && it.path.isNotBlank() }.distinctBy { it.path }

/** AII-197: the MIME type to open a file with: its own type, else a guess from the name, else anything. */
fun fileMime(p: Pic): String {
    if (p.type.contains('/') && p.type != "application/octet-stream") return p.type
    return when (p.name.ifEmpty { p.path.substringBefore('?') }.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "txt", "log", "md" -> "text/plain"
        "csv" -> "text/csv"
        "json" -> "application/json"
        "zip" -> "application/zip"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "mp3" -> "audio/mpeg"
        "mp4" -> "video/mp4"
        "apk" -> "application/vnd.android.package-archive"
        else -> "*/*"
    }
}
