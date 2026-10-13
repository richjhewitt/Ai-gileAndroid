package com.smizflark.aigile.messenger

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.util.UUID

/**
 * A real, throwaway AI-gile board for JVM tests: its own port and temp data, owner "Rich",
 * and a do-nothing agent so a message to a lead never wakes a real one. Never Rich's board.
 * Needs node on the PATH and the AI-gile folder next to this project (or AIGILE_BOARD); [find] returns null otherwise.
 */
class TestBoard private constructor(val url: String, private val proc: Process, private val dir: File) : AutoCloseable {

    private val http = OkHttpClient()

    /** Raw call, as an agent would make it (for seeding). Returns the body. */
    fun call(method: String, path: String, body: String? = null): String {
        val rb = Request.Builder().url(url + path)
        rb.method(method, body?.toRequestBody("application/json".toMediaType()))
        http.newCall(rb.build()).execute().use { r ->
            val text = r.body?.string() ?: ""
            check(r.isSuccessful) { "$method $path -> ${r.code} $text" }
            return text
        }
    }

    override fun close() {
        proc.destroy()
        proc.waitFor()
        dir.deleteRecursively()
    }

    companion object {
        fun find(): TestBoard? {
            val board = System.getenv("AIGILE_BOARD")?.let(::File)
                ?: generateSequence(File("").absoluteFile) { it.parentFile }
                    .mapNotNull { dir -> dir.parentFile?.let { File(it, "AI-gile") } }
                    .firstOrNull { File(it, "server.js").exists() }
                ?: return null
            if (!File(board, "server.js").exists()) return null

            val port = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }
            val dir = Files.createTempDirectory("aigile-testboard-").toFile()
            val win = System.getProperty("os.name").lowercase().contains("win")
            val stub = File(dir, if (win) "agent-stub.bat" else "agent-stub.sh").apply {
                writeText(if (win) "@echo off\r\n" else "#!/bin/sh\nexit 0\n"); setExecutable(true)
            }
            val local = File(dir, "aigile.local.json").apply { writeText("{}") }
            val token = UUID.randomUUID().toString()
            val pb = ProcessBuilder("node", File(board, "server.js").path).directory(board)
                .redirectErrorStream(true).redirectOutput(File(dir, "board.log"))
            val env: MutableMap<String, String> = pb.environment()
            listOf("AIGILE_DEV", "AIGILE_ALLOW_DONE", "AIGILE_AS", "AIGILE_URL").forEach { env.remove(it) }
            env += mapOf(
                "AIGILE_LOCAL" to local.path, "AIGILE_PORT" to port.toString(),
                "AIGILE_DATA" to File(dir, "data").also { it.mkdirs() }.path,
                "AIGILE_HOME" to File(dir, "projects").also { it.mkdirs() }.path,
                "AIGILE_USAGE_DIR" to File(dir, "usage").also { it.mkdirs() }.path,
                "AIGILE_AGENT_COMMAND" to stub.path, "AIGILE_NO_USAGE_AUTO" to "1",
                "AIGILE_OWNER" to "Rich", "AIGILE_QA_TOKEN" to token,
            )
            val proc = try { pb.start() } catch (e: java.io.IOException) { dir.deleteRecursively(); return null }
            val b = TestBoard("http://127.0.0.1:$port", proc, dir)
            // Only trust the server that hands back our token.
            repeat(100) {
                if (runCatching { b.call("GET", "/api/meta").contains(token) }.getOrDefault(false)) return b
                Thread.sleep(100)
            }
            b.close()
            error("test board did not start on port $port")
        }
    }
}
