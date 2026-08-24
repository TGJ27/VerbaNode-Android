package com.verbanode.mobile

import com.verbanode.mobile.network.ConnectionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2StateTest {
    @Test
    fun connectedIsDerivedFromExplicitConnectionState() {
        assertFalse(MobileUiState(connectionState = ConnectionState.DISCONNECTED).connected)
        assertFalse(MobileUiState(connectionState = ConnectionState.CONNECTING).connected)
        assertFalse(MobileUiState(connectionState = ConnectionState.RECONNECTING).connected)
        assertTrue(MobileUiState(connectionState = ConnectionState.CONNECTED).connected)
    }

    @Test
    fun busyIsDerivedFromOutstandingOperations() {
        assertFalse(MobileUiState().busy)
        assertTrue(MobileUiState(activeOperations = setOf("one")).busy)
        assertTrue(MobileUiState(activeOperations = setOf("one", "two")).busy)
    }
}
