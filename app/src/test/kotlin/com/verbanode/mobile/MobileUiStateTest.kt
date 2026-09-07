package com.verbanode.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiStateTest {
    @Test
    fun defaultsToDisconnectedIdleSnapshot() {
        val state = MobileUiState()
        assertFalse(state.connected)
        assertFalse(state.busy)
        assertFalse(state.recording)
        assertEquals("Disconnected", state.connectionLabel)
        assertEquals("idle", state.mode)
        assertTrue(state.knowledgeAllDocuments.isEmpty())
        assertFalse(state.knowledgeLoading)
        assertNull(state.knowledgeLoadError)
    }

    @Test
    fun connectionSnapshotKeepsExplicitConnectivityAndLabel() {
        val state = MobileUiState(connected = true, connectionLabel = "Connected")
        assertTrue(state.connected)
        assertEquals("Connected", state.connectionLabel)
    }

    @Test
    fun busyAndRecordingFlagsRemainIndependent() {
        assertTrue(MobileUiState(busy = true).busy)
        assertFalse(MobileUiState(busy = true).recording)
        assertTrue(MobileUiState(recording = true).recording)
        assertFalse(MobileUiState(recording = true).busy)
    }
}
