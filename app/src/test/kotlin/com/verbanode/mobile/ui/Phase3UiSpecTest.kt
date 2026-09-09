package com.verbanode.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class Phase3UiSpecTest {
    @Test
    fun managementScreensMatchApprovedPhaseThreeOrder() {
        assertEquals(
            listOf("Agents", "Agent Details", "Knowledge", "Scripts", "Audio", "Backups / Restore"),
            Phase3UiSpec.managementScreens,
        )
    }

    @Test
    fun chatStatusMapsInternalPipelineToCompactUserLabels() {
        assertEquals("Offline", Phase3UiSpec.chatStatusLabel(false, "Disconnected"))
        assertEquals("Ready", Phase3UiSpec.chatStatusLabel(true, "Ready"))
        assertEquals("Listening", Phase3UiSpec.chatStatusLabel(true, "Listening"))
        assertEquals("Transcribing", Phase3UiSpec.chatStatusLabel(true, "Transcribing"))
        assertEquals("Thinking", Phase3UiSpec.chatStatusLabel(true, "Generating"))
        assertEquals("Speaking", Phase3UiSpec.chatStatusLabel(true, "Preparing speech"))
    }
}
