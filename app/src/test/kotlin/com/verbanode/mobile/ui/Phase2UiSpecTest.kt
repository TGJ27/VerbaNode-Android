package com.verbanode.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Phase2UiSpecTest {
    @Test
    fun typeToTalkIsTtsOnly() {
        assertEquals(listOf("TTS / Voice"), Phase2UiSpec.typeToTalkSettings)
        assertFalse(Phase2UiSpec.typeToTalkHasModel)
        assertFalse(Phase2UiSpec.typeToTalkHasAgent)
        assertFalse(Phase2UiSpec.typeToTalkHasListeningOrTranscription)
    }

    @Test
    fun pushToTalkStatesMatchApprovedFlow() {
        assertEquals(
            listOf("Ready", "Listening", "Transcribing", "Sending", "Waiting", "Speaking"),
            Phase2UiSpec.pushToTalkStates,
        )
    }

    @Test
    fun pushToTalkVisualStateMapsPipelineLabels() {
        assertEquals("Listening", Phase2UiSpec.pushToTalkVisualState(recording = true, chatStatus = "Recording"))
        assertEquals("Transcribing", Phase2UiSpec.pushToTalkVisualState(recording = false, chatStatus = "Transcribing"))
        assertEquals("Sending", Phase2UiSpec.pushToTalkVisualState(recording = false, chatStatus = "Sending"))
        assertEquals("Waiting", Phase2UiSpec.pushToTalkVisualState(recording = false, chatStatus = "Generating"))
        assertEquals("Speaking", Phase2UiSpec.pushToTalkVisualState(recording = false, chatStatus = "Preparing speech"))
        assertEquals("Ready", Phase2UiSpec.pushToTalkVisualState(recording = false, chatStatus = "Ready"))
    }

    @Test
    fun chatAttachmentIsRemoved() {
        assertFalse(Phase2UiSpec.chatHasAttachment)
    }
}
