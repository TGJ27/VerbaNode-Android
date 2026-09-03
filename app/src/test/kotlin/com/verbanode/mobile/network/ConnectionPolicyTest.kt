package com.verbanode.mobile.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionPolicyTest {
    @Test
    fun reconnectBackoffIsExponentialAndCapped() {
        assertEquals(500L, reconnectDelayMs(0))
        assertEquals(1_000L, reconnectDelayMs(1))
        assertEquals(2_000L, reconnectDelayMs(2))
        assertEquals(4_000L, reconnectDelayMs(3))
        assertEquals(8_000L, reconnectDelayMs(4))
        assertEquals(10_000L, reconnectDelayMs(5))
        assertEquals(10_000L, reconnectDelayMs(20))
    }

    @Test
    fun authenticationCloseDoesNotReconnect() {
        assertFalse(shouldReconnectAfterClose(4401))
        assertTrue(shouldReconnectAfterClose(1000))
        assertTrue(shouldReconnectAfterClose(1006))
    }
}
