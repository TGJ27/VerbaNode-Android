package com.verbanode.mobile.network

import org.json.JSONArray
import org.json.JSONObject

data class ClientInfo(
    val product: String,
    val serverVersion: String,
    val build: String,
    val apiVersion: Int,
    val instanceId: String?,
    val instanceName: String?,
    val websocketVersion: Int,
    val certificateFingerprintSha256: String,
    val certificateSpkiSha256: String,
    val instanceDiscoveryEnabled: Boolean,
    val mobilePairing: Boolean,
    val trustedDevices: Boolean,
)

data class AuthSession(
    val token: String,
    val sessionId: String?,
    val clientName: String?,
    val deviceId: String?,
)

data class Agent(
    val id: Int,
    val name: String,
    val avatar: String,
    val color: String,
    val language: String,
)

data class ConversationSummary(val id: Int, val title: String?)

data class ChatMessage(
    val id: Int,
    val role: String,
    val content: String,
    val source: String?,
    val createdAt: String?,
)

data class BootstrapData(
    val agents: List<Agent>,
    val activeAgent: Agent?,
    val conversationId: Int?,
    val messages: List<ChatMessage>,
    val mode: String,
    val aiMode: String?,
    val sttMode: String?,
    val ttsMode: String?,
)

data class TrustedDevice(
    val deviceId: String,
    val name: String,
    val deviceType: String,
    val createdAt: String?,
    val lastSeenAt: String?,
    val revokedAt: String?,
    val trusted: Boolean,
    val activeController: Boolean,
)

data class PairingClaim(
    val deviceId: String,
    val deviceToken: String,
    val deviceName: String,
    val serverUrl: String,
    val spkiSha256: String,
)

data class ApiException(val status: Int, val code: String?, override val message: String) : Exception(message)

fun parseClientInfo(json: JSONObject): ClientInfo {
    val server = json.optJSONObject("server") ?: JSONObject()
    val api = json.optJSONObject("api") ?: JSONObject()
    val instance = json.optJSONObject("instance") ?: JSONObject()
    val ws = json.optJSONObject("websocket") ?: JSONObject()
    val tls = json.optJSONObject("tls") ?: JSONObject()
    val discovery = json.optJSONObject("discovery") ?: JSONObject()
    val features = json.optJSONObject("features") ?: JSONObject()
    return ClientInfo(
        product = json.optString("product"),
        serverVersion = server.optString("version"),
        build = server.optString("build"),
        apiVersion = api.optInt("version", 0),
        instanceId = instance.optString("id").ifBlank { null },
        instanceName = instance.optString("name").ifBlank { null },
        websocketVersion = ws.optInt("protocol_version", 0),
        certificateFingerprintSha256 = tls.optString("certificate_fingerprint_sha256").lowercase(),
        certificateSpkiSha256 = tls.optString("certificate_spki_sha256").lowercase(),
        instanceDiscoveryEnabled = discovery.optBoolean("enabled", false),
        mobilePairing = features.optBoolean("mobile_pairing", false),
        trustedDevices = features.optBoolean("trusted_devices", false),
    )
}

fun ClientInfo.requireAndroidCompatibility() {
    require(product == "VerbaNode") { "The selected server is not VerbaNode" }
    require(apiVersion == 1 && websocketVersion == 1) {
        "Unsupported VerbaNode protocol (API $apiVersion, WS $websocketVersion)"
    }
    require(mobilePairing && trustedDevices) {
        "VerbaNode Core v0.9.0 or newer is required for this Android controller"
    }
}

fun parseAgent(value: JSONObject): Agent = Agent(
    id = value.optInt("id"),
    name = value.optString("name", "Agent"),
    avatar = value.optString("avatar", "AI"),
    color = value.optString("color", "#6c63ff"),
    language = value.optString("language", "en"),
)

fun parseMessage(value: JSONObject): ChatMessage = ChatMessage(
    id = value.optInt("id"),
    role = value.optString("role"),
    content = value.optString("content"),
    source = value.optString("source").ifBlank { null },
    createdAt = value.optString("created_at").ifBlank { null },
)

private fun JSONArray.objects(): List<JSONObject> = buildList {
    for (index in 0 until length()) optJSONObject(index)?.let(::add)
}

fun parseBootstrap(json: JSONObject): BootstrapData {
    val agents = (json.optJSONArray("agents") ?: JSONArray()).objects().map(::parseAgent)
    val active = json.optJSONObject("active_agent")?.let(::parseAgent)
    val conversationId = json.optJSONObject("conversation")?.optInt("id")?.takeIf { it > 0 }
    val messages = (json.optJSONArray("messages") ?: JSONArray()).objects().map(::parseMessage)
    return BootstrapData(
        agents = agents,
        activeAgent = active,
        conversationId = conversationId,
        messages = messages,
        mode = json.optString("mode", "idle"),
        aiMode = json.optJSONObject("ai")?.optString("mode"),
        sttMode = json.optJSONObject("stt")?.optString("mode"),
        ttsMode = json.optJSONObject("tts")?.optString("mode"),
    )
}

fun parseDevices(json: JSONObject): List<TrustedDevice> =
    (json.optJSONArray("devices") ?: JSONArray()).objects().map { value ->
        TrustedDevice(
            deviceId = value.optString("device_id"),
            name = value.optString("name", "Device"),
            deviceType = value.optString("device_type", "mobile"),
            createdAt = value.optString("created_at").ifBlank { null },
            lastSeenAt = value.optString("last_seen_at").ifBlank { null },
            revokedAt = value.optString("revoked_at").ifBlank { null },
            trusted = value.optBoolean("trusted", false),
            activeController = value.optBoolean("active_controller", false),
        )
    }
