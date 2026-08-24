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
    val audioLibrary: Boolean,
    val configurationOptions: Boolean,
    val scriptQueueLoop: Boolean,
    val typeToTalkQueue: Boolean,
    val scriptDefaults: Boolean,
    val broadAudioFormats: Boolean,
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

class ApiProtocolException(message: String, cause: Throwable? = null) : Exception(message, cause)

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
        audioLibrary = features.optBoolean("audio_library", false),
        configurationOptions = features.optBoolean("configuration_options", false),
        scriptQueueLoop = features.optBoolean("script_queue_loop", false),
        typeToTalkQueue = features.optBoolean("type_to_talk_queue", false),
        scriptDefaults = features.optBoolean("script_defaults", false),
        broadAudioFormats = features.optBoolean("broad_audio_formats", false),
    )
}

fun ClientInfo.requireAndroidCompatibility() {
    require(product == "VerbaNode") { "The selected server is not VerbaNode" }
    require(apiVersion == 1 && websocketVersion == 1) {
        "Unsupported VerbaNode protocol (API $apiVersion, WS $websocketVersion)"
    }
    require(mobilePairing && trustedDevices && audioLibrary && configurationOptions && scriptQueueLoop && typeToTalkQueue && scriptDefaults) {
        "VerbaNode Core v0.9.2 or newer is required for this Android controller"
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

data class TypeToTalkItem(
    val id: Int,
    val text: String,
    val position: Int,
    val status: String,
    val createdAt: String?,
)

data class AudioLibraryItem(
    val name: String,
    val sizeBytes: Long,
    val modifiedAt: String?,
    val durationSeconds: Double?,
    val playing: Boolean,
)

fun parseTypeToTalkItems(array: JSONArray): List<TypeToTalkItem> = array.objects().map { value ->
    TypeToTalkItem(
        id = value.optInt("id"),
        text = value.optString("text"),
        position = value.optInt("position", 0),
        status = value.optString("status", "waiting"),
        createdAt = value.optString("created_at").ifBlank { null },
    )
}

fun parseAudioLibraryItems(array: JSONArray): List<AudioLibraryItem> = array.objects().mapNotNull { value ->
    val name = value.optString("name").trim()
    if (name.isBlank()) return@mapNotNull null
    AudioLibraryItem(
        name = name,
        sizeBytes = value.optLong("size_bytes", 0L),
        modifiedAt = value.optString("modified_at").ifBlank { null },
        durationSeconds = if (value.has("duration_seconds") && !value.isNull("duration_seconds")) {
            value.optDouble("duration_seconds").takeIf { it.isFinite() && it >= 0.0 }
        } else {
            null
        },
        playing = value.optBoolean("playing", false),
    )
}

data class ScriptItem(
    val id: Int,
    val title: String,
    val text: String,
    val enabled: Boolean,
    val language: String,
    val ttsMode: String,
    val edgeVoice: String,
    val kokoroVoiceId: Int,
    val ttsRate: Double,
    val ttsVolume: Double,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("text", text)
        .put("enabled", enabled)
        .put("language", language)
        .put("tts_mode", ttsMode)
        .put("edge_voice", edgeVoice)
        .put("kokoro_voice_id", kokoroVoiceId)
        .put("tts_rate", ttsRate)
        .put("tts_volume", ttsVolume)
}

data class ScriptQueueItem(
    val id: Int,
    val scriptId: Int,
    val position: Int,
    val status: String,
    val pauseAfterSeconds: Double,
    val title: String,
)

fun parseScriptItems(array: JSONArray): List<ScriptItem> = array.objects().map { value ->
    ScriptItem(
        id = value.optInt("id"),
        title = value.optString("title", "Script"),
        text = value.optString("text"),
        enabled = value.optBoolean("enabled", true),
        language = value.optString("language", "en"),
        ttsMode = value.optString("tts_mode", "edge"),
        edgeVoice = value.optString("edge_voice", "en-US-AriaNeural"),
        kokoroVoiceId = value.optInt("kokoro_voice_id", 0),
        ttsRate = value.optDouble("tts_rate", 1.0),
        ttsVolume = value.optDouble("tts_volume", 1.0),
    )
}

fun parseScriptQueueItems(array: JSONArray): List<ScriptQueueItem> = array.objects().map { value ->
    ScriptQueueItem(
        id = value.optInt("id"),
        scriptId = value.optInt("script_id"),
        position = value.optInt("position", 0),
        status = value.optString("status", "waiting"),
        pauseAfterSeconds = value.optDouble("pause_after_seconds", 0.0),
        title = value.optString("title", value.optString("script_title", "Queued script")),
    )
}
