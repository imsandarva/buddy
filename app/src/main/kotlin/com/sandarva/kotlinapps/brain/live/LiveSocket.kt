package com.sandarva.kotlinapps.brain.live

import com.sandarva.kotlinapps.debug.BuddyLog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Thin OkHttp WebSocket for Gemini Live. First send must be setup; wait for setupComplete. */
class LiveSocket(
    private val apiKey: String,
    private val listener: Listener,
    private val http: OkHttpClient = client()
) {
    interface Listener {
        fun onSetupComplete()
        fun onAudio(pcm: ByteArray)
        fun onInterrupted()
        fun onToolCall(calls: List<LiveFunctionCall>)
        fun onTranscript(text: String, fromUser: Boolean)
        fun onTurnComplete()
        fun onClosed(reason: String)
    }

    private var socket: WebSocket? = null
    private val open = AtomicBoolean(false)
    private val ready = AtomicBoolean(false)
    @Volatile private var closed = false
    private val inbound = Executors.newSingleThreadExecutor { task -> Thread(task, "buddy-live-in").apply { isDaemon = true } }

    fun connect() {
        closed = false
        val request = Request.Builder().url("${LiveConfig.WS}?key=$apiKey").build()
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                BuddyLog.d("Live.socket", "open")
                open.set(true)
                val setup = LiveMessages.setup()
                val sent = webSocket.send(setup)
                BuddyLog.d("Live.socket", "setup sent=$sent chars=${setup.length}")
            }
            /** Gemini Live replies on binary JSON frames; decode off the reader so audio stays paced. */
            override fun onMessage(webSocket: WebSocket, text: String) = postIn { handle(text) }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = postIn { handle(bytes.utf8()) }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                BuddyLog.d("Live.socket", "closing code=$code reason=$reason")
                webSocket.close(1000, null)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                BuddyLog.d("Live.socket", "closed code=$code reason=$reason")
                fail("closed")
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                BuddyLog.e("Live.socket", t.message ?: "fail", t)
                fail(t.message ?: "connection failed")
            }
        })
    }

    fun send(json: String): Boolean {
        val ws = socket ?: return false
        if (!open.get()) return false
        return ws.send(json)
    }

    fun close() {
        closed = true
        open.set(false)
        ready.set(false)
        inbound.shutdownNow()
        socket?.close(1000, "bye")
        socket = null
    }

    val isReady: Boolean get() = ready.get()

    private fun handle(text: String) {
        val msg = try { JSONObject(text) } catch (_: Exception) {
            BuddyLog.e("Live.socket", "bad json ${text.take(120)}")
            return
        }
        when {
            msg.has("setupComplete") || msg.has("setup_complete") -> {
                BuddyLog.d("Live.socket", "setupComplete")
                ready.set(true)
                listener.onSetupComplete()
            }
            msg.has("error") -> {
                val err = msg.optJSONObject("error")
                val message = err?.optString("message")?.ifBlank { null } ?: "live error"
                BuddyLog.e("Live.socket", "error code=${err?.opt("code")} $message")
                fail(message)
            }
            msg.has("toolCall") || msg.has("tool_call") -> {
                val calls = LiveMessages.parseToolCalls(msg)
                BuddyLog.d("Live.socket", "toolCall n=${calls.size} names=${calls.map { it.name }}")
                if (calls.isNotEmpty()) listener.onToolCall(calls)
            }
            msg.has("serverContent") -> onServer(msg.optJSONObject("serverContent") ?: return)
            msg.has("server_content") -> onServer(msg.optJSONObject("server_content") ?: return)
            msg.has("goAway") || msg.has("go_away") -> fail("server asked to disconnect")
            else -> BuddyLog.d("Live.socket", "keys=${msg.keys().asSequence().joinToString()}")
        }
    }

    private fun postIn(block: () -> Unit) {
        try { inbound.execute { if (!closed) block() } } catch (_: Exception) { }
    }

    private fun onServer(content: JSONObject) {
        if (content.optBoolean("interrupted")) listener.onInterrupted()
        val turn = content.optJSONObject("modelTurn") ?: content.optJSONObject("model_turn")
        val parts = turn?.optJSONArray("parts")
        LiveMessages.parseAudio(parts).forEach { listener.onAudio(it) }
        if (content.optBoolean("turnComplete") || content.optBoolean("turn_complete") || content.optBoolean("generationComplete") || content.optBoolean("generation_complete")) {
            listener.onTurnComplete()
        }
        content.optJSONObject("inputTranscription")?.optString("text")?.takeIf { it.isNotBlank() }?.let { listener.onTranscript(it, true) }
        content.optJSONObject("input_transcription")?.optString("text")?.takeIf { it.isNotBlank() }?.let { listener.onTranscript(it, true) }
        content.optJSONObject("outputTranscription")?.optString("text")?.takeIf { it.isNotBlank() }?.let { listener.onTranscript(it, false) }
        content.optJSONObject("output_transcription")?.optString("text")?.takeIf { it.isNotBlank() }?.let { listener.onTranscript(it, false) }
    }

    private fun fail(reason: String) {
        if (closed) return
        closed = true
        open.set(false)
        ready.set(false)
        listener.onClosed(reason)
    }

    companion object {
        fun client(): OkHttpClient = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
