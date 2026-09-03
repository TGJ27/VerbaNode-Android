package com.verbanode.mobile.network

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProtocolModelsTest {
    private fun validClientInfo(): JSONObject = JSONObject()
        .put("contract_version", 1)
        .put("product", "VerbaNode")
        .put("server", JSONObject().put("version", "0.12.1").put("build", "release"))
        .put("instance", JSONObject().put("id", "instance-1").put("name", "VerbaNode"))
        .put("api", JSONObject().put("version", 1))
        .put("websocket", JSONObject().put("protocol_version", 1))
        .put(
            "tls",
            JSONObject()
                .put("certificate_fingerprint_sha256", "a".repeat(64))
                .put("certificate_spki_sha256", "b".repeat(64)),
        )
        .put("discovery", JSONObject().put("enabled", true))
        .put(
            "features",
            JSONObject()
                .put("mobile_pairing", true)
                .put("trusted_devices", true)
                .put("audio_library", true)
                .put("configuration_options", true)
                .put("script_queue_loop", true)
                .put("type_to_talk_queue", true)
                .put("script_defaults", true)
                .put("broad_audio_formats", true)
                .put("knowledge_management", true),
        )

    private fun assertProtocolError(block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertNotNull("Expected protocol failure", error)
        assertEquals("ApiProtocolException", error!!.javaClass.simpleName)
    }

    @Test
    fun clientInfoRequiresApiVersion() {
        val payload = validClientInfo().apply { getJSONObject("api").remove("version") }
        assertProtocolError { parseClientInfo(payload) }
    }

    @Test
    fun clientInfoRejectsWrongFeatureType() {
        val payload = validClientInfo().apply { getJSONObject("features").put("mobile_pairing", "yes") }
        assertProtocolError { parseClientInfo(payload) }
    }

    @Test
    fun bootstrapRequiresAgentsArray() {
        val payload = JSONObject()
            .put("messages", JSONArray())
            .put("mode", "idle")
        assertProtocolError { parseBootstrap(payload) }
    }

    @Test
    fun bootstrapRequiresMode() {
        val payload = JSONObject()
            .put("agents", JSONArray())
            .put("messages", JSONArray())
        assertProtocolError { parseBootstrap(payload) }
    }

    @Test
    fun devicesRequireDeviceIdentity() {
        val payload = JSONObject().put(
            "devices",
            JSONArray().put(JSONObject().put("name", "Phone").put("device_type", "mobile")),
        )
        assertProtocolError { parseDevices(payload) }
    }
    @Test
    fun validClientInfoParsesCompatibilityFields() {
        val info = parseClientInfo(validClientInfo())
        assertEquals("VerbaNode", info.product)
        assertEquals("0.12.1", info.serverVersion)
        assertEquals(1, info.apiVersion)
        assertEquals(1, info.websocketVersion)
        assertTrue(info.mobilePairing)
        assertTrue(info.knowledgeManagement)
    }

    @Test
    fun androidCompatibilityRejectsUnsupportedApiVersion() {
        val info = parseClientInfo(validClientInfo().apply { getJSONObject("api").put("version", 2) })
        assertProtocolError { info.requireAndroidCompatibility() }
    }

    @Test
    fun agentParserRestoresLegacyDefaults() {
        val agent = parseAgent(JSONObject().put("id", 7).put("name", "Assistant"))
        assertEquals(7, agent.id)
        assertEquals("Assistant", agent.name)
        assertEquals("AI", agent.avatar)
        assertEquals("#6c63ff", agent.color)
        assertEquals("en", agent.language)
    }

    @Test
    fun agentParserRejectsNonPositiveIdentity() {
        assertProtocolError { parseAgent(JSONObject().put("id", 0).put("name", "Assistant")) }
    }

    @Test
    fun bootstrapParsesCurrentConversationAndMessages() {
        val payload = JSONObject()
            .put("agents", JSONArray().put(JSONObject().put("id", 1).put("name", "A")))
            .put("active_agent", JSONObject().put("id", 1).put("name", "A"))
            .put("conversation", JSONObject().put("id", 9))
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("id", 4)
                        .put("role", "assistant")
                        .put("content", "Hello")
                        .put("source", JSONObject.NULL)
                        .put("created_at", JSONObject.NULL),
                ),
            )
            .put("mode", "conversation")
            .put("ai", JSONObject().put("mode", "thinking"))
            .put("stt", JSONObject().put("mode", "idle"))
            .put("tts", JSONObject().put("mode", "speaking"))

        val bootstrap = parseBootstrap(payload)
        assertEquals(1, bootstrap.agents.size)
        assertEquals(1, bootstrap.activeAgent?.id)
        assertEquals(9, bootstrap.conversationId)
        assertEquals("Hello", bootstrap.messages.single().content)
        assertNull(bootstrap.messages.single().source)
        assertEquals("thinking", bootstrap.aiMode)
        assertEquals("idle", bootstrap.sttMode)
        assertEquals("speaking", bootstrap.ttsMode)
    }

    @Test
    fun deviceParserKeepsOptionalStateDefaults() {
        val devices = parseDevices(
            JSONObject().put(
                "devices",
                JSONArray().put(JSONObject().put("device_id", "phone-1")),
            ),
        )
        val device = devices.single()
        assertEquals("phone-1", device.deviceId)
        assertEquals("Device", device.name)
        assertEquals("mobile", device.deviceType)
        assertFalse(device.trusted)
        assertFalse(device.activeController)
    }

}
