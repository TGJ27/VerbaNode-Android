package com.verbanode.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPolishPolicyTest {
    @Test
    fun agentActionsAreSplitAcrossCompactRows() {
        assertEquals(listOf(listOf("Activate", "Clear memory"), listOf("Backup", "Delete")), UiPolishPolicy.agentActionRows(active = false))
        assertEquals(listOf(listOf("Clear memory", "Backup"), listOf("Delete")), UiPolishPolicy.agentActionRows(active = true))
        assertTrue(UiPolishPolicy.agentActionRows(false).all { it.size <= 2 })
    }

    @Test
    fun scriptTransportLabelsStayShortAndSingleLine() {
        assertEquals(listOf("Play", "Pause", "Stop", "Loop"), UiPolishPolicy.scriptTransportLabels)
        assertTrue(UiPolishPolicy.scriptTransportLabels.all { !it.contains(' ') && it.length <= 5 })
    }

    @Test
    fun immersiveBarsUseTransientSwipeReveal() {
        assertTrue(UiPolishPolicy.immersiveSystemBars)
        assertTrue(UiPolishPolicy.transientBarsBySwipe)
    }
}
