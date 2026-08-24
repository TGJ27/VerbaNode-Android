package com.verbanode.mobile.network

import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class VerbaNodeWebSocket(
    private val api: VerbaNodeApi,
    private val sessionToken: String,
    private val onEvent: (String, JSONObject?) -> Unit,
    private val onState: (ConnectionState, String) -> Unit,
    private val onProtocolError: (String) -> Unit,
    private val onSessionLost: () -> Unit,
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val stateLock = Any()
    private var socket: WebSocket? = null
    private var heartbeat: ScheduledFuture<*>? = null
    private var reconnect: ScheduledFuture<*>? = null
    private var connecting = false
    @Volatile private var closed = false
    private var reconnectAttempts = 0

    /**
     * Start (or resume) the transport without performing network I/O on the
     * caller. In particular, /api/auth/ws-ticket is fetched on the dedicated
     * transport executor rather than the Android main thread.
     */
    fun connect() {
        scheduleConnect(0L)
    }

    private fun scheduleConnect(delayMs: Long) {
        synchronized(stateLock) {
            if (closed || connecting || reconnect?.isDone == false) return
            reconnect = scheduler.schedule({ beginConnectAttempt() }, delayMs, TimeUnit.MILLISECONDS)
        }
    }

    private fun beginConnectAttempt() {
        val shouldConnect = synchronized(stateLock) {
            reconnect = null
            if (closed || connecting) {
                false
            } else {
                connecting = true
                true
            }
        }
        if (shouldConnect) openSocket()
    }

    private fun openSocket() {
        if (closed) {
            synchronized(stateLock) { connecting = false }
            return
        }
        val reconnecting = synchronized(stateLock) { reconnectAttempts > 0 }
        onState(
            if (reconnecting) ConnectionState.RECONNECTING else ConnectionState.CONNECTING,
            if (reconnecting) "Reconnecting" else "Connecting",
        )
        try {
            val ticket = api.wsTicket(sessionToken)
            if (closed) {
                synchronized(stateLock) { connecting = false }
                return
            }
            val wsUrl = api.baseUrl.replaceFirst("https://", "wss://") +
                "/ws?ticket=" + URLEncoder.encode(ticket, "UTF-8")
            val request = Request.Builder().url(wsUrl).build()
            val candidate = api.client.newWebSocket(request, listener)
            val keep = synchronized(stateLock) {
                if (closed) {
                    connecting = false
                    false
                } else {
                    socket = candidate
                    true
                }
            }
            if (!keep) candidate.cancel()
        } catch (error: Exception) {
            synchronized(stateLock) { connecting = false }
            if (!closed) {
                onState(ConnectionState.RECONNECTING, error.message ?: "WebSocket connection failed")
                if (error is ApiException && error.status == 401) {
                    onSessionLost()
                } else {
                    scheduleReconnect()
                }
            }
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val accepted = synchronized(stateLock) {
                if (closed || (socket != null && socket !== webSocket)) {
                    false
                } else {
                    socket = webSocket
                    connecting = false
                    reconnectAttempts = 0
                    reconnect?.cancel(false)
                    reconnect = null
                    true
                }
            }
            if (!accepted) {
                webSocket.close(1000, "Superseded")
                return
            }
            onState(ConnectionState.CONNECTED, "Connected")
            startHeartbeat(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val payload = JSONObject(text)
                val type = payload.optString("type", payload.optString("event")).trim()
                require(type.isNotBlank()) { "WebSocket event is missing a type" }
                val rawData = payload.opt("data")
                val data = when (rawData) {
                    null, JSONObject.NULL -> null
                    is JSONObject -> rawData
                    else -> error("WebSocket event data must be a JSON object")
                }
                onEvent(type, data)
            } catch (error: Exception) {
                onProtocolError(error.message ?: "Malformed WebSocket event")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!markDisconnected(webSocket)) return
            if (closed) return
            if (code == 4401) {
                onState(ConnectionState.DISCONNECTED, reason.ifBlank { "Session ended" })
                onSessionLost()
            } else {
                onState(ConnectionState.RECONNECTING, reason.ifBlank { "Reconnecting" })
                scheduleReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!markDisconnected(webSocket)) return
            if (closed) return
            onState(ConnectionState.RECONNECTING, t.message ?: "Connection lost")
            scheduleReconnect()
        }
    }

    private fun markDisconnected(webSocket: WebSocket): Boolean = synchronized(stateLock) {
        if (socket !== webSocket) return@synchronized false
        socket = null
        connecting = false
        heartbeat?.cancel(false)
        heartbeat = null
        true
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        synchronized(stateLock) {
            heartbeat?.cancel(false)
            heartbeat = scheduler.scheduleAtFixedRate({
                if (!closed && socket === webSocket) {
                    val payload = JSONObject()
                        .put("protocol", 1)
                        .put("type", "command.heartbeat")
                        .put("request_id", UUID.randomUUID().toString())
                    webSocket.send(payload.toString())
                }
            }, 10, 15, TimeUnit.SECONDS)
        }
    }

    private fun scheduleReconnect() {
        synchronized(stateLock) {
            if (closed || connecting || reconnect?.isDone == false) return
            val attempt = reconnectAttempts++.coerceAtMost(5)
            val delayMs = (500L shl attempt).coerceAtMost(10_000L)
            reconnect = scheduler.schedule({ beginConnectAttempt() }, delayMs, TimeUnit.MILLISECONDS)
        }
    }

    fun close() {
        val current = synchronized(stateLock) {
            if (closed) return
            closed = true
            connecting = false
            heartbeat?.cancel(false)
            heartbeat = null
            reconnect?.cancel(false)
            reconnect = null
            socket.also { socket = null }
        }
        current?.close(1000, "Client closed")
        scheduler.shutdownNow()
    }
}
