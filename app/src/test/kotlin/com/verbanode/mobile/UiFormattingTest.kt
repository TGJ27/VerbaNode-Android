package com.verbanode.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormattingTest {
    @Test
    fun pipelineStatusUsesConversationContextForIdle() {
        assertEquals("Listening", pipelineStatusLabel("idle", "conversation"))
        assertEquals("Ready", pipelineStatusLabel("idle", "idle"))
    }

    @Test
    fun pipelineStatusMapsKnownAndUnknownStages() {
        assertEquals("Generating", pipelineStatusLabel("thinking", "idle"))
        assertEquals("Running tool", pipelineStatusLabel("tooling", "idle"))
        assertEquals("Dense index build", pipelineStatusLabel("dense_index_build", "idle"))
    }

    @Test
    fun modeStatusMapsConversationAndPttModes() {
        assertEquals("Listening", modeStatusLabel("conversation"))
        assertEquals("Recording", modeStatusLabel("ptt"))
        assertEquals("Recording", modeStatusLabel("browser_ptt"))
        assertEquals("Transcribing", modeStatusLabel("processing"))
        assertEquals("Ready", modeStatusLabel("idle"))
    }

    @Test
    fun friendlyErrorPrefersMessageAndFallsBackToType() {
        assertEquals("boom", friendlyError(IllegalStateException("boom")))
        assertEquals("IllegalStateException", friendlyError(IllegalStateException()))
    }

    @Test
    fun friendlyErrorAcceptsGenericThrowable() {
        assertEquals("refresh failed", friendlyError(Throwable("refresh failed")))
    }
}
