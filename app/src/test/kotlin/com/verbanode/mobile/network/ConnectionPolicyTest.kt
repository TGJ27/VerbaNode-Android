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
    fun terminalContractCloseCodesDoNotReconnect() {
        assertFalse(shouldReconnectAfterClose(4401))
        assertFalse(shouldReconnectAfterClose(4403))
        assertFalse(shouldReconnectAfterClose(4406))
        assertTrue(shouldReconnectAfterClose(4408))
        assertTrue(shouldReconnectAfterClose(1000))
        assertTrue(shouldReconnectAfterClose(1006))
    }
}
