package com.verbanode.mobile.network

import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class VerbaNodeWebSocket(
    private val api: VerbaNodeApi,
    private val sessionToken: String,
    private val onEvent: (String, JSONObject?) -> Unit,
    private val onState: (Boolean, String) -> Unit,
    private val onProtocolError: (String) -> Unit,
    private val onSessionLost: () -> Unit,
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var socket: WebSocket? = null
    private var heartbeat: ScheduledFuture<*>? = null
    @Volatile private var closed = false
    private var reconnectAttempts = 0

    fun connect() {
        if (closed) return
        try {
            val ticket = api.wsTicket(sessionToken)
            val wsUrl = api.baseUrl.replaceFirst("https://", "wss://") + "/ws?ticket=" + java.net.URLEncoder.encode(ticket, "UTF-8")
            val request = Request.Builder().url(wsUrl).build()
            socket = api.client.newWebSocket(request, listener)
        } catch (error: Exception) {
            onState(false, error.message ?: "WebSocket connection failed")
            scheduleReconnect()
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempts = 0
            onState(true, "Connected")
            startHeartbeat(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val event = try {
                parseWebSocketEvent(text)
            } catch (error: ApiProtocolException) {
                onProtocolError(error.message ?: "Malformed WebSocket event")
                return
            }
            onEvent(event.type, event.data as? JSONObject)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            heartbeat?.cancel(false)
            heartbeat = null
            onState(false, reason.ifBlank { "Disconnected" })
            if (code == 4401) onSessionLost() else scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            heartbeat?.cancel(false)
            heartbeat = null
            onState(false, t.message ?: "Connection lost")
            scheduleReconnect()
        }
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        heartbeat?.cancel(false)
        heartbeat = scheduler.scheduleAtFixedRate({
            if (!closed) {
                val payload = JSONObject()
                    .put("protocol", 1)
                    .put("type", "command.heartbeat")
                    .put("request_id", UUID.randomUUID().toString())
                webSocket.send(payload.toString())
            }
        }, 10, 15, TimeUnit.SECONDS)
    }

    private fun scheduleReconnect() {
        if (closed) return
        val attempt = reconnectAttempts++.coerceAtMost(5)
        val delayMs = (500L shl attempt).coerceAtMost(10_000L)
        scheduler.schedule({ if (!closed) connect() }, delayMs, TimeUnit.MILLISECONDS)
    }

    fun close() {
        closed = true
        heartbeat?.cancel(false)
        heartbeat = null
        socket?.close(1000, "Client closed")
        socket = null
        scheduler.shutdownNow()
    }
}
