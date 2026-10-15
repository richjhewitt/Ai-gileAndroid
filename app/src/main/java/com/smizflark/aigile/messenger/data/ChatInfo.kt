package com.smizflark.aigile.messenger.data

/** What the chat details view lists (AII-80): the cards still open and the pictures sent in the chat. */
data class ChatInfo(val openCards: List<Target>, val pictures: List<Pic>, val links: List<ChatLink> = emptyList(), val files: List<Pic> = emptyList(), val about: About? = null)

/** Open cards are the ones waiting on you; pictures are newest first, each only once. */
fun chatInfo(c: Conversation?): ChatInfo {
    if (c == null) return ChatInfo(emptyList(), emptyList())
    val open = c.targets.filter { it.id != null && it.waiting }
    val pics = c.bubbles.asReversed().flatMap { it.pics }.filter { it.image }.distinctBy { it.path }
    return ChatInfo(open, pics, Links.of(c.bubbles.map { it.text }), files(c), about(c))
}

/** AII-197: a file is an attachment that is not a picture. */
fun isFile(p: Pic): Boolean = !p.image && p.path.isNotBlank()

/** AII-197: every file that is not a picture, newest first, each once. */
fun files(c: Conversation): List<Pic> = c.bubbles.asReversed().flatMap { it.pics }.filter { isFile(it) }.distinctBy { it.path }

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

/** AII-203: how many messages the app has for a chat, how many are yours, and the day of the oldest. */
data class About(val total: Int, val mine: Int, val since: java.time.LocalDate?)

/** Only messages that have reached the chat count (not waiting, sending or failed ones). */
fun about(c: Conversation, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): About? {
    val sent = c.bubbles.filter { it.state == SendState.SENT }
    if (sent.isEmpty()) return null
    val first = sent.mapNotNull { runCatching { java.time.Instant.parse(it.at) }.getOrNull() }.minOrNull()
    return About(sent.size, sent.count { it.mine }, first?.atZone(zone)?.toLocalDate())
}

/** "3 Oct" in the phone's language; the year joins it when it is not this year. */
fun aboutDate(d: java.time.LocalDate, today: java.time.LocalDate = Now.today(), locale: java.util.Locale = java.util.Locale.getDefault()): String =
    java.time.format.DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMM" else "d MMM yyyy", locale).format(d)
