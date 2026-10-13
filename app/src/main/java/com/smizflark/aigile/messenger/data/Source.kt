package com.smizflark.aigile.messenger.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/** Where chats come from: the real board, or the built-in demo. */
interface ChatSource {
    suspend fun owner(): String
    suspend fun chats(): List<ChatRef>
    suspend fun conversation(ref: ChatRef): Conversation
    /** Sends a reply. Returns the card it landed on (a new 💬 card when target is null), if the board says. */
    suspend fun send(ref: ChatRef, text: String, target: String?, files: List<Upload> = emptyList()): String?
    /** A new 💬 message to a project's team. Returns the new card id. */
    suspend fun newMessage(slug: String, text: String, files: List<Upload> = emptyList()): String?
    /** One card of a lead's chat (AII-45); null where the board can't say. */
    suspend fun card(slug: String, id: String): CardDetail? = null
}

/** A picture or file to send with a message. */
class Upload(val name: String, val type: String, val bytes: ByteArray)

/** A message for the person holding the phone, not a stack trace. */
class BoardError(message: String, val code: Int = 0) : Exception(message)

val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; explicitNulls = false }

class BoardSource(
    val base: String,
    private val key: String,
    client: OkHttpClient? = null,
    /** Header sent to a board on this PC in tests; the board only answers to "localhost". */
    private val hostHeader: String? = null,
    deviceId: String = "",
) : ChatSource {

    companion object { const val SEALED = "/api/phone/sealed" }

    val http: OkHttpClient = client ?: OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var ownerName: String? = null

    /** Device id from the pairing QR; with it the link is sealed once /api/meta says "sealed": 1. */
    private val deviceId: String = deviceId.trim()
    private val link: SealedLink? = if (this.deviceId.isNotEmpty() && key.isNotBlank())
        runCatching { SealedLink(this.deviceId, key) }.getOrNull() else null
    /** null until asked: does this board take sealed calls? */
    @Volatile private var sealedOn: Boolean? = if (link == null) false else null
    private val probe = Mutex()

    /** True when calls go through POST /api/phone/sealed. */
    suspend fun sealed(): Boolean {
        sealedOn?.let { return it }
        return probe.withLock {
            sealedOn ?: run {
                // Ask /api/meta sealed first so the key never travels; an older board has no sealed endpoint (404).
                val on = try {
                    val meta = json.decodeFromString<Meta>(sealedCall("GET", "/api/meta", null))
                    meta.sealed == 1
                } catch (e: BoardError) { if (e.code == 404 || e.code == 405) false else throw e }
                sealedOn = on
                on
            }
        }
    }

    private suspend fun call(method: String, path: String, body: JsonObject? = null): String =
        if (sealed()) sealedCall(method, path, body) else plainCall(method, path, body)

    private suspend fun plainCall(method: String, path: String, body: JsonObject?): String = exchange(method, path, body?.toString(), bearer = true)

    /** One inner call, sealed with S: only the device id travels in the clear. */
    private suspend fun sealedCall(method: String, path: String, body: JsonObject?): String {
        val l = link ?: throw BoardError("This phone has no sealed pairing. Pair the phone again.")
        val text = exchange("POST", SEALED, l.request(method, path, body), bearer = false)
        val inner = try { l.response(text) }
            catch (e: Exception) { throw BoardError("The board's answer didn't open with this phone's key. Pair the phone again.") }
        val b = inner.body.toString()
        if (inner.status !in 200..299) throw BoardError(explain(inner.status, b), inner.status)
        return b
    }

    private suspend fun exchange(method: String, path: String, body: String?, bearer: Boolean): String = withContext(Dispatchers.IO) {
        val rb = Request.Builder().url(base + path)
        if (bearer && key.isNotBlank()) rb.header("Authorization", "Bearer $key")
        if (!bearer) link?.let { rb.header("X-Aigile-Device", it.deviceId) }
        hostHeader?.let { rb.header("Host", it) }
        rb.header("Accept", "application/json")
        if (body != null) rb.method(method, body.toRequestBody("application/json".toMediaType()))
        else rb.method(method, null)
        try {
            http.newCall(rb.build()).execute().use { r ->
                val text = r.body?.string() ?: ""
                if (!r.isSuccessful) throw BoardError(explain(r.code, text), r.code)
                text
            }
        } catch (e: BoardError) { throw e }
        catch (e: UnknownHostException) { throw BoardError("Can't find the board. Is Tailscale on, on this phone?") }
        catch (e: ConnectException) { throw BoardError("Can't reach the board. Is the PC on and Tailscale running?") }
        catch (e: SocketTimeoutException) { throw BoardError("The board took too long to answer. Try again.") }
        catch (e: SSLException) { throw BoardError("Secure connection failed. Check the board address.") }
        catch (e: IOException) { throw BoardError("Connection problem: ${e.message ?: "no answer"}") }
        catch (e: IllegalArgumentException) { throw BoardError("That board address doesn't look right.") }
    }

    private fun explain(code: Int, body: String): String {
        val msg = runCatching { json.parseToJsonElement(body).let { (it as JsonObject)["error"]?.jsonPrimitive?.content } }.getOrNull()
        return when {
            code == 403 && msg == "wrong host" -> "The board doesn't let phones in yet. It needs the phone pairing update (AIG-907)."
            code == 401 || code == 403 -> "The board didn't accept this phone's key. Pair the phone again."
            code == 404 -> "Not found on the board (is it up to date?)"
            else -> msg ?: "The board said $code"
        }
    }

    override suspend fun owner(): String =
        ownerName ?: json.decodeFromString<Meta>(call("GET", "/api/meta")).owner.also { ownerName = it }

    override suspend fun chats(): List<ChatRef> = coroutineScope {
        val leads = async { json.decodeFromString<List<ChatSummary>>(call("GET", "/api/chats")) }
        // Friend DMs are optional: a board with DMs off still lists its leads.
        val friends = async { runCatching { json.decodeFromString<List<Contact>>(call("GET", "/api/dms/contacts")) }.getOrDefault(emptyList()) }
        Mapper.chats(leads.await(), friends.await())
    }

    override suspend fun conversation(ref: ChatRef): Conversation = when (ref.kind) {
        Kind.FRIEND -> Mapper.friend(ref, owner(), json.decodeFromString(call("GET", "/api/dms/contacts/${ref.id}/messages")))
        else -> Mapper.conversation(ref, json.decodeFromString(call("GET", "/api/chats/${ref.id}")))
    }

    override suspend fun send(ref: ChatRef, text: String, target: String?, files: List<Upload>): String? = when {
        // Friend DMs carry their files inside the sealed message.
        ref.kind == Kind.FRIEND -> {
            call("POST", "/api/dms/contacts/${ref.id}/messages", buildJsonObject {
                put("text", text)
                if (files.isNotEmpty()) put("attachments", buildJsonArray {
                    files.forEach { f -> add(buildJsonObject { put("name", f.name); put("type", f.type); put("data", b64(f.bytes)) }) }
                })
            }); null
        }
        ref.kind == Kind.WATCHMAN -> {
            val ids = upload("_watchman", files)
            call("POST", "/api/chats/_watchman", buildJsonObject { put("text", text); if (ids.isNotEmpty()) put("attachments", ids) }); null
        }
        target != null -> {
            val ids = upload(ref.id, files)
            call("POST", "/api/projects/${ref.id}/cards/$target/comments", buildJsonObject {
                put("author", owner()); put("text", text); if (ids.isNotEmpty()) put("attachments", ids)
            })
            target
        }
        else -> newMessage(ref.id, text, files)
    }

    /** Board messages take files uploaded first, then named by id (AIG-77). */
    private suspend fun upload(slug: String, files: List<Upload>): JsonArray = buildJsonArray {
        for (f in files) {
            val saved = json.decodeFromString<Attachment>(call("POST", "/api/projects/$slug/attachments",
                buildJsonObject { put("name", f.name); put("type", f.type); put("data", b64(f.bytes)) }))
            add(buildJsonObject { put("id", saved.id); put("type", f.type) })
        }
    }

    private fun b64(bytes: ByteArray) = java.util.Base64.getEncoder().encodeToString(bytes)

    override suspend fun newMessage(slug: String, text: String, files: List<Upload>): String? {
        val ids = upload(slug, files)
        val body = buildJsonObject {
            put("title", if (text.isBlank()) "💬 📷 Picture" else Mapper.quickTitle(text)); put("description", text); put("type", "task")
            put("status", "team"); put("createdBy", owner()); put("quick", JsonPrimitive(true))
            if (ids.isNotEmpty()) put("attachments", ids)
        }
        return json.decodeFromString<Created>(call("POST", "/api/projects/$slug/cards", body)).id
    }

    override suspend fun card(slug: String, id: String): CardDetail? = try {
        json.decodeFromString<CardDetail>(call("GET", "/api/projects/$slug/cards/$id"))
    } catch (e: BoardError) {
        if (e.code == 404) null else throw e   // 404: a board from before AII-45, the view just shows the messages
    }

    /** Full URL for a board picture path like /api/avatars/x.png. */
    fun url(path: String) = if (path.startsWith("http")) path else base + path
}
