package com.smizflark.aigile.messenger.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/** AII-38: keep the last crash on the phone, and turn it (or a bug typed by hand) into a message for the app's team. */
object Crash {
    /** The AI-gile Android team on the board: bug reports land there as a 💬 message. */
    const val TEAM = "ai-gile-android"
    private const val FILE = "last-crash.txt"
    private const val MAX = 6000   // a card description, not a log dump

    /** Saves the stack trace of a crash, then lets Android close the app as usual. */
    fun install(context: Context) {
        val dir = context.applicationContext.filesDir
        val before = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { File(dir, FILE).writeText(Instant.now().toString() + "\n" + trace(e)) }
            before?.uncaughtException(thread, e)
        }
    }

    /** The last crash (first line: when), or null. */
    fun last(context: Context): String? = File(context.filesDir, FILE).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    fun clear(context: Context) { File(context.filesDir, FILE).delete() }

    fun trace(e: Throwable): String = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()

    /** The message sent to the team: what you typed, then the crash in a code block, then version and phone. */
    fun report(note: String, crash: String?, version: String, phone: String = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"): String {
        val lines = mutableListOf<String>()
        val said = note.trim()
        lines += if (said.isNotEmpty()) said else if (crash != null) "The app crashed" else "Bug report"
        lines += ""
        lines += "- App ${version}, $phone"
        if (crash != null) {
            val at = crash.lineSequence().first()
            val body = crash.substringAfter('\n').trimEnd()
            lines += ""
            lines += "- Crashed at $at"
            lines += ""
            lines += "```"
            lines += if (body.length > MAX) body.take(MAX) + "\n…" else body
            lines += "```"
        }
        return lines.joinToString("\n")
    }
}
