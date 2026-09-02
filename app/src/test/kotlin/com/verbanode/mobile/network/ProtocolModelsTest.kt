package com.verbanode.mobile.network

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
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
}
