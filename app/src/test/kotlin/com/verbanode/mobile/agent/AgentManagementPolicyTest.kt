package com.verbanode.mobile.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentManagementPolicyTest {
    @Test
    fun normalizesFullAgentEditorValuesWithinCoreSchemaBounds() {
        val normalized = normalizeAgentDraft(
            AgentDraft(
                name = "  Receptionist  ", avatar = "  RC  ", color = " #112233 ",
                role = "  Front desk  ", systemPrompt = "  Be concise.  ", greeting = "  Hello  ",
                llmModel = "  qwen3.5:0.8b  ", language = "en", ttsMode = "kokoro_fallback",
                edgeVoice = " en-US-AriaNeural ", kokoroVoiceId = 999, ttsRate = 9.0, ttsVolume = -2.0,
                sttModel = " iic/SenseVoiceSmall ", temperature = 5.0, topP = -1.0,
                maxTokens = 9000, contextSize = 100, toolsEnabled = setOf("get_weather", "", "get_location"),
                knowledgeLibraryIds = setOf(9, -1, 3),
            ),
        )
        assertEquals("Receptionist", normalized.name)
        assertEquals("RC", normalized.avatar)
        assertEquals(102, normalized.kokoroVoiceId)
        assertEquals(2.0, normalized.ttsRate, 0.0)
        assertEquals(0.0, normalized.ttsVolume, 0.0)
        assertEquals(2.0, normalized.temperature, 0.0)
        assertEquals(0.0, normalized.topP, 0.0)
        assertEquals(8192, normalized.maxTokens)
        assertEquals(512, normalized.contextSize)
        assertEquals(setOf("get_location", "get_weather"), normalized.toolsEnabled)
        assertEquals(setOf(3, 9), normalized.knowledgeLibraryIds)
    }

    @Test
    fun searchMatchesNameRoleAndModel() {
        assertTrue(agentMatchesQuery("Ropi", "Robotics receptionist", "qwen3.5:0.8b", "robotics"))
        assertTrue(agentMatchesQuery("Ropi", "Robotics receptionist", "qwen3.5:0.8b", "QWEN"))
        assertFalse(agentMatchesQuery("Ropi", "Robotics receptionist", "qwen3.5:0.8b", "finance"))
    }

    @Test
    fun loadErrorToolsAreHiddenButGloballyDisabledToolsRemainVisible() {
        val options = agentToolOptions(
            listOf(
                AgentToolOption("weather", "Weather", enabled = true, status = "healthy"),
                AgentToolOption("broken", "Broken", enabled = true, status = "load_error"),
                AgentToolOption("disabled", "Disabled", enabled = false, status = "healthy"),
            ),
        )
        assertEquals(listOf("disabled", "weather"), options.map { it.id })
        assertFalse(options.first().enabled)
    }

    @Test
    fun defaultToolsMatchCoreAgentDefaults() {
        assertEquals(
            setOf("get_current_time", "get_location", "get_weather", "handle_exit_intent"),
            DEFAULT_AGENT_TOOL_IDS,
        )
    }
}
