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
        .put("api", JSONObject().put("version", 1).put("minimum_supported_version", 1))
        .put(
            "authentication",
            JSONObject()
                .put("login_endpoint", "/api/auth/login")
                .put("device_login_endpoint", "/api/auth/device-login")
                .put("logout_endpoint", "/api/auth/logout")
                .put("session_header", "X-Session-Token")
                .put("pairing_claim_endpoint", "/api/pairing/claim"),
        )
        .put(
            "websocket",
            JSONObject()
                .put("endpoint", "/ws")
                .put("protocol_version", 1)
                .put("ticket_endpoint", "/api/auth/ws-ticket")
                .put("ticket_required", true)
                .put("heartbeat_interval_seconds", 15.0)
                .put("heartbeat_timeout_seconds", 45.0),
        )
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
        .put("mobile_contract_fingerprint", AndroidCoreContract.EXPECTED_FINGERPRINT)
        .put("mobile_contract", AndroidCoreContract.toJson())

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
    fun androidCompatibilityRejectsServerThatDroppedApiV1() {
        val info = parseClientInfo(
            validClientInfo().apply {
                getJSONObject("api").put("version", 2).put("minimum_supported_version", 2)
                getJSONObject("mobile_contract").put("api_version", 2).put("minimum_api_version", 2)
            },
        )
        assertProtocolError { info.requireAndroidCompatibility() }
    }


    @Test
    fun authGrantParsesNegotiatedProtocolAndHeartbeat() {
        val session = parseAuthSession(
            JSONObject()
                .put("token", "token-1")
                .put("server_version", "0.12.2")
                .put("api_version", 1)
                .put("websocket_protocol_version", 1)
                .put("heartbeat_interval_seconds", 12.5)
                .put("heartbeat_timeout_seconds", 40.0)
                .put("session", JSONObject().put("session_id", "session-1").put("client_name", "Pixel")),
        )
        assertEquals("token-1", session.token)
        assertEquals(1, session.apiVersion)
        assertEquals(1, session.websocketProtocolVersion)
        assertEquals(12.5, session.heartbeatIntervalSeconds, 0.0)
        assertEquals(40.0, session.heartbeatTimeoutSeconds, 0.0)
    }

    @Test
    fun authGrantRejectsMissingNegotiationMetadata() {
        assertProtocolError { parseAuthSession(JSONObject().put("token", "token-1")) }
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
