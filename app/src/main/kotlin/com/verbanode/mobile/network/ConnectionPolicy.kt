package com.verbanode.mobile.network

internal enum class WebSocketCloseAction {
    RECONNECT,
    SESSION_LOST,
    PROTOCOL_ERROR,
}

internal fun reconnectDelayMs(failedAttempts: Int): Long {
    val attempt = failedAttempts.coerceIn(0, 5)
    return (500L shl attempt).coerceAtMost(10_000L)
}

internal fun webSocketCloseAction(code: Int): WebSocketCloseAction = when (code) {
    AndroidCoreContract.WS_CLOSE_UNAUTHORIZED -> WebSocketCloseAction.SESSION_LOST
    AndroidCoreContract.WS_CLOSE_ORIGIN_REJECTED,
    AndroidCoreContract.WS_CLOSE_PROTOCOL_UNSUPPORTED -> WebSocketCloseAction.PROTOCOL_ERROR
    else -> WebSocketCloseAction.RECONNECT
}

internal fun shouldReconnectAfterClose(code: Int): Boolean = webSocketCloseAction(code) == WebSocketCloseAction.RECONNECT
