package com.verbanode.mobile.network

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileContractTest {

    @Test
    fun canonicalFingerprintMatchesCoreRelease() {
        assertEquals(AndroidCoreContract.EXPECTED_FINGERPRINT, AndroidCoreContract.fingerprint())
        assertTrue(AndroidCoreContract.endpoints.containsKey("diagnostics_logs_get"))
    }
    private fun assertProtocolError(block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertNotNull("Expected protocol failure", error)
        assertEquals("ApiProtocolException", error!!.javaClass.simpleName)
    }

    private fun clientInfoWithContract(): JSONObject = JSONObject()
        .put("contract_version", 1)
        .put("product", "VerbaNode")
        .put("server", JSONObject().put("version", "0.12.4").put("build", "release"))
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

    @Test
    fun canonicalContractAcceptsCurrentCoreManifest() {
        val info = parseClientInfo(clientInfoWithContract())
        info.requireAndroidCompatibility()
        assertEquals(AndroidCoreContract.CONTRACT_VERSION, info.mobileContract.contractVersion)
        assertEquals(AndroidCoreContract.endpoints.size, info.mobileContract.endpoints.size)
    }


    @Test
    fun fingerprintMismatchIsRejectedBeforeAuthentication() {
        val payload = clientInfoWithContract().put("mobile_contract_fingerprint", "f".repeat(64))
        val info = parseClientInfo(payload)
        assertProtocolError { info.requireAndroidCompatibility() }
    }

    @Test
    fun changedServerEndpointIsRejectedBeforeAuthentication() {
        val payload = clientInfoWithContract()
        payload.getJSONObject("mobile_contract")
            .getJSONObject("endpoints")
            .getJSONObject("auth_login")
            .put("path", "/api/auth/login-v2")
        val info = parseClientInfo(payload)
        assertProtocolError { info.requireAndroidCompatibility() }
    }

    @Test
    fun incompatibleSessionHeaderIsRejected() {
        val payload = clientInfoWithContract()
        payload.getJSONObject("authentication").put("session_header", "Authorization")
        val info = parseClientInfo(payload)
        assertProtocolError { info.requireAndroidCompatibility() }
    }

    @Test
    fun endpointMatcherCoversDynamicCoreRoutes() {
        assertTrue(AndroidCoreContract.matches("POST", "/api/agents/12/activate"))
        assertTrue(AndroidCoreContract.matches("POST", "/api/agents/generate-role"))
        assertTrue(AndroidCoreContract.matches("GET", "/api/knowledge/documents/9/content?limit=100"))
        assertTrue(AndroidCoreContract.matches("POST", "/api/knowledge/documents/9/reingest"))
        assertTrue(AndroidCoreContract.matches("GET", "/api/knowledge/jobs"))
        assertTrue(AndroidCoreContract.matches("PUT", "/api/knowledge/agents/4/libraries"))
        assertTrue(AndroidCoreContract.matches("POST", "/api/models/pull/qwen3.5:0.8b"))
        assertTrue(AndroidCoreContract.matches("PATCH", "/api/audio-library/greeting.mp3"))
        assertFalse(AndroidCoreContract.matches("POST", "/api/does-not-exist"))
    }
}
