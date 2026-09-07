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
    private val heartbeatIntervalSeconds: Double,
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
            val wsUrl = api.baseUrl.replaceFirst("https://", "wss://") + AndroidCoreContract.WEBSOCKET_ENDPOINT + "?ticket=" + java.net.URLEncoder.encode(ticket, "UTF-8")
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
            when (webSocketCloseAction(code)) {
                WebSocketCloseAction.RECONNECT -> scheduleReconnect()
                WebSocketCloseAction.SESSION_LOST -> onSessionLost()
                WebSocketCloseAction.PROTOCOL_ERROR -> onProtocolError(
                    when (code) {
                        AndroidCoreContract.WS_CLOSE_PROTOCOL_UNSUPPORTED -> "VerbaNode WebSocket protocol is incompatible with this Android version"
                        AndroidCoreContract.WS_CLOSE_ORIGIN_REJECTED -> "VerbaNode rejected the native WebSocket connection"
                        else -> reason.ifBlank { "VerbaNode WebSocket contract error" }
                    },
                )
            }
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
                    .put("protocol", AndroidCoreContract.WEBSOCKET_PROTOCOL_VERSION)
                    .put("type", "command.heartbeat")
                    .put("request_id", UUID.randomUUID().toString())
                webSocket.send(payload.toString())
            }
        }, heartbeatIntervalMillis(), heartbeatIntervalMillis(), TimeUnit.MILLISECONDS)
    }


    private fun heartbeatIntervalMillis(): Long =
        (heartbeatIntervalSeconds * 1000.0).toLong().coerceAtLeast(1_000L)

    private fun scheduleReconnect() {
        if (closed) return
        val delayMs = reconnectDelayMs(reconnectAttempts++)
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
