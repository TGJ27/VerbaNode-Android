package com.verbanode.mobile.network

import org.json.JSONArray
import org.json.JSONObject

private const val CLIENT_INFO_CONTEXT = "/api/client-info"
private const val BOOTSTRAP_CONTEXT = "/api/bootstrap"
private const val DEVICES_CONTEXT = "/api/devices"
private val SHA256_HEX = Regex("^[0-9a-fA-F]{64}$")

data class ClientInfo(
    val product: String,
    val serverVersion: String,
    val build: String,
    val apiVersion: Int,
    val minimumApiVersion: Int,
    val instanceId: String?,
    val instanceName: String?,
    val authenticationLoginEndpoint: String,
    val authenticationDeviceLoginEndpoint: String,
    val authenticationLogoutEndpoint: String,
    val sessionHeader: String,
    val pairingClaimEndpoint: String,
    val websocketEndpoint: String,
    val websocketVersion: Int,
    val websocketTicketEndpoint: String,
    val websocketTicketRequired: Boolean,
    val heartbeatIntervalSeconds: Double,
    val heartbeatTimeoutSeconds: Double,
    val certificateFingerprintSha256: String,
    val certificateSpkiSha256: String,
    val instanceDiscoveryEnabled: Boolean,
    val mobilePairing: Boolean,
    val trustedDevices: Boolean,
    val audioLibrary: Boolean,
    val configurationOptions: Boolean,
    val scriptQueueLoop: Boolean,
    val typeToTalkQueue: Boolean,
    val scriptDefaults: Boolean,
    val broadAudioFormats: Boolean,
    val knowledgeManagement: Boolean,
    val mobileContractFingerprint: String,
    val mobileContract: MobileContract,
)

data class AuthSession(
    val token: String,
    val sessionId: String?,
    val clientName: String?,
    val deviceId: String?,
    val serverVersion: String,
    val apiVersion: Int,
    val websocketProtocolVersion: Int,
    val heartbeatIntervalSeconds: Double,
    val heartbeatTimeoutSeconds: Double,
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

private fun requireSha256(value: String, field: String, context: String): String {
    if (!SHA256_HEX.matches(value)) protocolError(context, "field '$field' must be a 64-character SHA-256 hex value")
    return value.lowercase()
}

fun parseClientInfo(json: JSONObject): ClientInfo {
    val contractVersion = json.requireInt("contract_version", CLIENT_INFO_CONTEXT)
    if (contractVersion != 1) protocolError(CLIENT_INFO_CONTEXT, "unsupported client-info contract version $contractVersion")

    val server = json.requireObject("server", CLIENT_INFO_CONTEXT)
    val api = json.requireObject("api", CLIENT_INFO_CONTEXT)
    val instance = json.optionalObject("instance", CLIENT_INFO_CONTEXT)
    val authentication = json.requireObject("authentication", CLIENT_INFO_CONTEXT)
    val ws = json.requireObject("websocket", CLIENT_INFO_CONTEXT)
    val tls = json.requireObject("tls", CLIENT_INFO_CONTEXT)
    val discovery = json.requireObject("discovery", CLIENT_INFO_CONTEXT)
    val features = json.requireObject("features", CLIENT_INFO_CONTEXT)
    val mobileContract = parseMobileContract(json.requireObject("mobile_contract", CLIENT_INFO_CONTEXT))

    return ClientInfo(
        product = json.requireString("product", CLIENT_INFO_CONTEXT),
        serverVersion = server.requireString("version", "$CLIENT_INFO_CONTEXT.server"),
        build = server.requireString("build", "$CLIENT_INFO_CONTEXT.server"),
        apiVersion = api.requireInt("version", "$CLIENT_INFO_CONTEXT.api"),
        minimumApiVersion = api.requireInt("minimum_supported_version", "$CLIENT_INFO_CONTEXT.api"),
        instanceId = instance?.optionalString("id", "$CLIENT_INFO_CONTEXT.instance"),
        instanceName = instance?.optionalString("name", "$CLIENT_INFO_CONTEXT.instance"),
        authenticationLoginEndpoint = authentication.requireString("login_endpoint", "$CLIENT_INFO_CONTEXT.authentication"),
        authenticationDeviceLoginEndpoint = authentication.requireString("device_login_endpoint", "$CLIENT_INFO_CONTEXT.authentication"),
        authenticationLogoutEndpoint = authentication.requireString("logout_endpoint", "$CLIENT_INFO_CONTEXT.authentication"),
        sessionHeader = authentication.requireString("session_header", "$CLIENT_INFO_CONTEXT.authentication"),
        pairingClaimEndpoint = authentication.requireString("pairing_claim_endpoint", "$CLIENT_INFO_CONTEXT.authentication"),
        websocketEndpoint = ws.requireString("endpoint", "$CLIENT_INFO_CONTEXT.websocket"),
        websocketVersion = ws.requireInt("protocol_version", "$CLIENT_INFO_CONTEXT.websocket"),
        websocketTicketEndpoint = ws.requireString("ticket_endpoint", "$CLIENT_INFO_CONTEXT.websocket"),
        websocketTicketRequired = ws.requireBoolean("ticket_required", "$CLIENT_INFO_CONTEXT.websocket"),
        heartbeatIntervalSeconds = ws.requirePositiveDouble("heartbeat_interval_seconds", "$CLIENT_INFO_CONTEXT.websocket"),
        heartbeatTimeoutSeconds = ws.requirePositiveDouble("heartbeat_timeout_seconds", "$CLIENT_INFO_CONTEXT.websocket"),
        certificateFingerprintSha256 = requireSha256(
            tls.requireString("certificate_fingerprint_sha256", "$CLIENT_INFO_CONTEXT.tls"),
            "certificate_fingerprint_sha256",
            "$CLIENT_INFO_CONTEXT.tls",
        ),
        certificateSpkiSha256 = requireSha256(
            tls.requireString("certificate_spki_sha256", "$CLIENT_INFO_CONTEXT.tls"),
            "certificate_spki_sha256",
            "$CLIENT_INFO_CONTEXT.tls",
        ),
        instanceDiscoveryEnabled = discovery.requireBoolean("enabled", "$CLIENT_INFO_CONTEXT.discovery"),
        mobilePairing = features.requireBoolean("mobile_pairing", "$CLIENT_INFO_CONTEXT.features"),
        trustedDevices = features.requireBoolean("trusted_devices", "$CLIENT_INFO_CONTEXT.features"),
        audioLibrary = features.requireBoolean("audio_library", "$CLIENT_INFO_CONTEXT.features"),
        configurationOptions = features.requireBoolean("configuration_options", "$CLIENT_INFO_CONTEXT.features"),
        scriptQueueLoop = features.requireBoolean("script_queue_loop", "$CLIENT_INFO_CONTEXT.features"),
        typeToTalkQueue = features.requireBoolean("type_to_talk_queue", "$CLIENT_INFO_CONTEXT.features"),
        scriptDefaults = features.requireBoolean("script_defaults", "$CLIENT_INFO_CONTEXT.features"),
        broadAudioFormats = features.requireBoolean("broad_audio_formats", "$CLIENT_INFO_CONTEXT.features"),
        knowledgeManagement = features.requireBoolean("knowledge_management", "$CLIENT_INFO_CONTEXT.features"),
        mobileContractFingerprint = requireSha256(json.requireString("mobile_contract_fingerprint", CLIENT_INFO_CONTEXT), "mobile_contract_fingerprint", CLIENT_INFO_CONTEXT),
        mobileContract = mobileContract,
    )
}

fun ClientInfo.requireAndroidCompatibility() {
    if (product != "VerbaNode") {
        protocolError(CLIENT_INFO_CONTEXT, "the selected server identifies as '$product', not VerbaNode")
    }
    if (!(minimumApiVersion <= AndroidCoreContract.API_VERSION && AndroidCoreContract.API_VERSION <= apiVersion)) {
        protocolError(CLIENT_INFO_CONTEXT, "Android API ${AndroidCoreContract.API_VERSION} is outside server range $minimumApiVersion..$apiVersion")
    }
    if (websocketVersion != AndroidCoreContract.WEBSOCKET_PROTOCOL_VERSION) {
        protocolError(CLIENT_INFO_CONTEXT, "unsupported WebSocket protocol version $websocketVersion")
    }
    if (authenticationLoginEndpoint != AndroidCoreContract.endpoint("auth_login").path ||
        authenticationDeviceLoginEndpoint != AndroidCoreContract.endpoint("auth_device_login").path ||
        authenticationLogoutEndpoint != AndroidCoreContract.endpoint("auth_logout").path
    ) {
        protocolError(CLIENT_INFO_CONTEXT, "authentication endpoint contract changed")
    }
    if (sessionHeader != AndroidCoreContract.SESSION_HEADER) protocolError(CLIENT_INFO_CONTEXT, "session header changed to '$sessionHeader'")
    if (pairingClaimEndpoint != AndroidCoreContract.endpoint("pairing_claim").path) protocolError(CLIENT_INFO_CONTEXT, "pairing claim endpoint changed to '$pairingClaimEndpoint'")
    if (websocketEndpoint != AndroidCoreContract.WEBSOCKET_ENDPOINT || websocketTicketEndpoint != AndroidCoreContract.WEBSOCKET_TICKET_ENDPOINT || !websocketTicketRequired) {
        protocolError(CLIENT_INFO_CONTEXT, "WebSocket connection contract changed")
    }
    AndroidCoreContract.validate(mobileContract)
    if (mobileContractFingerprint != AndroidCoreContract.EXPECTED_FINGERPRINT || AndroidCoreContract.fingerprint() != AndroidCoreContract.EXPECTED_FINGERPRINT) {
        protocolError(CLIENT_INFO_CONTEXT, "mobile contract fingerprint mismatch")
    }
    if (!(mobilePairing && trustedDevices && audioLibrary && configurationOptions && scriptQueueLoop && typeToTalkQueue && scriptDefaults && knowledgeManagement)) {
        protocolError(CLIENT_INFO_CONTEXT, "required VerbaNode mobile capabilities are unavailable")
    }
}

fun parseAuthSession(payload: JSONObject, context: String = "/api/auth/login"): AuthSession {
    val apiVersion = payload.requireInt("api_version", context)
    val websocketVersion = payload.requireInt("websocket_protocol_version", context)
    if (apiVersion != AndroidCoreContract.API_VERSION) protocolError(context, "server granted API version $apiVersion")
    if (websocketVersion != AndroidCoreContract.WEBSOCKET_PROTOCOL_VERSION) protocolError(context, "server granted WebSocket protocol $websocketVersion")
    val session = payload.requireObject("session", context)
    return AuthSession(
        token = payload.requireString("token", context),
        sessionId = session.optionalString("session_id", "$context.session"),
        clientName = session.optionalString("client_name", "$context.session"),
        deviceId = session.optionalString("device_id", "$context.session"),
        serverVersion = payload.requireString("server_version", context),
        apiVersion = apiVersion,
        websocketProtocolVersion = websocketVersion,
        heartbeatIntervalSeconds = payload.requirePositiveDouble("heartbeat_interval_seconds", context),
        heartbeatTimeoutSeconds = payload.requirePositiveDouble("heartbeat_timeout_seconds", context),
    )
}

private fun parseAgent(value: JSONObject, context: String): Agent {
    val id = value.requireInt("id", context)
    if (id <= 0) protocolError(context, "field 'id' must be a positive integer")
    return Agent(
        id = id,
        name = value.requireString("name", context),
        avatar = value.optionalString("avatar", context, blankAsNull = false) ?: "AI",
        color = value.optionalString("color", context, blankAsNull = false) ?: "#6c63ff",
        language = value.optionalString("language", context, blankAsNull = false) ?: "en",
    )
}

fun parseAgent(value: JSONObject): Agent = parseAgent(value, "agent")

private fun parseMessage(value: JSONObject, context: String): ChatMessage {
    val id = value.requireInt("id", context)
    if (id <= 0) protocolError(context, "field 'id' must be a positive integer")
    return ChatMessage(
        id = id,
        role = value.requireString("role", context),
        content = value.requireString("content", context, allowBlank = true),
        source = value.optionalString("source", context),
        createdAt = value.optionalString("created_at", context),
    )
}

fun parseMessage(value: JSONObject): ChatMessage = parseMessage(value, "message")

private fun optionalMode(parent: JSONObject, field: String): String? {
    val objectContext = "$BOOTSTRAP_CONTEXT.$field"
    return parent.optionalObject(field, BOOTSTRAP_CONTEXT)?.optionalString("mode", objectContext)
}

fun parseBootstrap(json: JSONObject): BootstrapData {
    val agents = json.requireArray("agents", BOOTSTRAP_CONTEXT).requireObjects("$BOOTSTRAP_CONTEXT.agents")
        .mapIndexed { index, value -> parseAgent(value, "$BOOTSTRAP_CONTEXT.agents[$index]") }
    val active = json.optionalObject("active_agent", BOOTSTRAP_CONTEXT)?.let { parseAgent(it, "$BOOTSTRAP_CONTEXT.active_agent") }
    val conversationId = json.optionalObject("conversation", BOOTSTRAP_CONTEXT)?.let { conversation ->
        val id = conversation.requireInt("id", "$BOOTSTRAP_CONTEXT.conversation")
        if (id <= 0) protocolError("$BOOTSTRAP_CONTEXT.conversation", "field 'id' must be a positive integer")
        id
    }
    val messages = json.requireArray("messages", BOOTSTRAP_CONTEXT).requireObjects("$BOOTSTRAP_CONTEXT.messages")
        .mapIndexed { index, value -> parseMessage(value, "$BOOTSTRAP_CONTEXT.messages[$index]") }
    return BootstrapData(
        agents = agents,
        activeAgent = active,
        conversationId = conversationId,
        messages = messages,
        mode = json.requireString("mode", BOOTSTRAP_CONTEXT),
        aiMode = optionalMode(json, "ai"),
        sttMode = optionalMode(json, "stt"),
        ttsMode = optionalMode(json, "tts"),
    )
}

fun parseDevices(json: JSONObject): List<TrustedDevice> =
    json.requireArray("devices", DEVICES_CONTEXT).requireObjects("$DEVICES_CONTEXT.devices").mapIndexed { index, value ->
        val context = "$DEVICES_CONTEXT.devices[$index]"
        TrustedDevice(
            deviceId = value.requireString("device_id", context),
            name = value.optionalString("name", context, blankAsNull = false) ?: "Device",
            deviceType = value.optionalString("device_type", context, blankAsNull = false) ?: "mobile",
            createdAt = value.optionalString("created_at", context),
            lastSeenAt = value.optionalString("last_seen_at", context),
            revokedAt = value.optionalString("revoked_at", context),
            trusted = value.optionalBoolean("trusted", context),
            activeController = value.optionalBoolean("active_controller", context),
        )
    }
