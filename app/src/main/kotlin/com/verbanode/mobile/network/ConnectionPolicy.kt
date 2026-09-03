package com.verbanode.mobile.network

internal const val SESSION_LOST_CLOSE_CODE = 4401

internal fun reconnectDelayMs(failedAttempts: Int): Long {
    val attempt = failedAttempts.coerceIn(0, 5)
    return (500L shl attempt).coerceAtMost(10_000L)
}

internal fun shouldReconnectAfterClose(code: Int): Boolean = code != SESSION_LOST_CLOSE_CODE
