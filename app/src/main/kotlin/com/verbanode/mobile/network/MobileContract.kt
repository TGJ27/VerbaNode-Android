package com.verbanode.mobile.network

import org.json.JSONArray
import org.json.JSONObject

data class ContractEndpoint(val method: String, val path: String)

data class MobileContract(
    val contractVersion: Int,
    val apiVersion: Int,
    val minimumApiVersion: Int,
    val websocketProtocolVersion: Int,
    val sessionHeader: String,
    val websocketEndpoint: String,
    val websocketTicketEndpoint: String,
    val endpoints: Map<String, ContractEndpoint>,
    val requestFields: Map<String, Set<String>>,
    val responseFields: Map<String, Set<String>>,
    val websocketCloseCodes: Map<String, Int>,
)

object AndroidCoreContract {
    const val CONTRACT_VERSION = 1
    const val API_VERSION = 1
    const val WEBSOCKET_PROTOCOL_VERSION = 1
    const val SESSION_HEADER = "X-Session-Token"
    const val WEBSOCKET_ENDPOINT = "/ws"
    const val WEBSOCKET_TICKET_ENDPOINT = "/api/auth/ws-ticket"
    const val WS_CLOSE_UNAUTHORIZED = 4401
    const val WS_CLOSE_ORIGIN_REJECTED = 4403
    const val WS_CLOSE_PROTOCOL_UNSUPPORTED = 4406
    const val WS_CLOSE_HEARTBEAT_TIMEOUT = 4408

    val endpoints: Map<String, ContractEndpoint> = linkedMapOf(
        "client_info" to ContractEndpoint("GET", "/api/client-info"),
        "auth_login" to ContractEndpoint("POST", "/api/auth/login"),
        "auth_device_login" to ContractEndpoint("POST", "/api/auth/device-login"),
        "auth_logout" to ContractEndpoint("POST", "/api/auth/logout"),
        "auth_ws_ticket" to ContractEndpoint("POST", "/api/auth/ws-ticket"),
        "bootstrap" to ContractEndpoint("GET", "/api/bootstrap"),
        "status" to ContractEndpoint("GET", "/api/status"),
        "pipeline" to ContractEndpoint("GET", "/api/pipeline"),
        "capabilities" to ContractEndpoint("GET", "/api/capabilities"),
        "actions" to ContractEndpoint("GET", "/api/actions"),
        "agents_list" to ContractEndpoint("GET", "/api/agents"),
        "agents_create" to ContractEndpoint("POST", "/api/agents"),
        "agent_update" to ContractEndpoint("PUT", "/api/agents/{agent_id}"),
        "agent_delete" to ContractEndpoint("DELETE", "/api/agents/{agent_id}"),
        "agent_activate" to ContractEndpoint("POST", "/api/agents/{agent_id}/activate"),
        "agent_memory_clear" to ContractEndpoint("DELETE", "/api/agents/{agent_id}/memory"),
        "agent_backup" to ContractEndpoint("GET", "/api/agents/{agent_id}/backup"),
        "conversation_create" to ContractEndpoint("POST", "/api/conversations"),
        "conversation_get" to ContractEndpoint("GET", "/api/conversations/{conversation_id}"),
        "conversation_clear" to ContractEndpoint("DELETE", "/api/conversations/{conversation_id}/messages"),
        "agent_conversations" to ContractEndpoint("GET", "/api/agents/{agent_id}/conversations"),
        "chat_send" to ContractEndpoint("POST", "/api/chat/send"),
        "conversation_mode_start" to ContractEndpoint("POST", "/api/conversation/start"),
        "conversation_mode_stop" to ContractEndpoint("POST", "/api/conversation/stop"),
        "conversation_settings" to ContractEndpoint("PUT", "/api/conversation/settings"),
        "browser_ptt_start" to ContractEndpoint("POST", "/api/browser-ptt/start"),
        "browser_ptt_audio" to ContractEndpoint("POST", "/api/browser-ptt/audio"),
        "browser_ptt_cancel" to ContractEndpoint("POST", "/api/browser-ptt/cancel"),
        "tts_stop" to ContractEndpoint("POST", "/api/tts/stop"),
        "devices_list" to ContractEndpoint("GET", "/api/devices"),
        "pairing_start" to ContractEndpoint("POST", "/api/devices/pairing/start"),
        "pairing_claim" to ContractEndpoint("POST", "/api/pairing/claim"),
        "device_rename" to ContractEndpoint("PATCH", "/api/devices/{device_id}"),
        "device_revoke" to ContractEndpoint("POST", "/api/devices/{device_id}/revoke"),
        "device_delete" to ContractEndpoint("DELETE", "/api/devices/{device_id}"),
        "knowledge_status" to ContractEndpoint("GET", "/api/knowledge/status"),
        "knowledge_libraries_list" to ContractEndpoint("GET", "/api/knowledge/libraries"),
        "knowledge_library_create" to ContractEndpoint("POST", "/api/knowledge/libraries"),
        "knowledge_library_update" to ContractEndpoint("PUT", "/api/knowledge/libraries/{library_id}"),
        "knowledge_library_delete" to ContractEndpoint("DELETE", "/api/knowledge/libraries/{library_id}"),
        "knowledge_documents_list" to ContractEndpoint("GET", "/api/knowledge/documents"),
        "knowledge_document_content" to ContractEndpoint("GET", "/api/knowledge/documents/{document_id}/content"),
        "knowledge_text_create" to ContractEndpoint("POST", "/api/knowledge/libraries/{library_id}/text-documents"),
        "knowledge_text_update" to ContractEndpoint("PUT", "/api/knowledge/documents/{document_id}/text"),
        "knowledge_document_delete" to ContractEndpoint("DELETE", "/api/knowledge/documents/{document_id}"),
        "knowledge_document_reindex" to ContractEndpoint("POST", "/api/knowledge/documents/{document_id}/reindex"),
        "knowledge_document_reingest" to ContractEndpoint("POST", "/api/knowledge/documents/{document_id}/reingest"),
        "knowledge_jobs" to ContractEndpoint("GET", "/api/knowledge/jobs"),
        "knowledge_agent_libraries_get" to ContractEndpoint("GET", "/api/knowledge/agents/{agent_id}/libraries"),
        "knowledge_agent_libraries_set" to ContractEndpoint("PUT", "/api/knowledge/agents/{agent_id}/libraries"),
        "knowledge_index_rebuild" to ContractEndpoint("POST", "/api/knowledge/index/rebuild"),
        "knowledge_search" to ContractEndpoint("POST", "/api/knowledge/search"),
        "knowledge_document_upload" to ContractEndpoint("POST", "/api/knowledge/libraries/{library_id}/documents"),
        "script_defaults_get" to ContractEndpoint("GET", "/api/scripts/defaults"),
        "script_defaults_put" to ContractEndpoint("PUT", "/api/scripts/defaults"),
        "scripts_list" to ContractEndpoint("GET", "/api/scripts"),
        "script_create" to ContractEndpoint("POST", "/api/scripts"),
        "script_update" to ContractEndpoint("PUT", "/api/scripts/{script_id}"),
        "script_delete" to ContractEndpoint("DELETE", "/api/scripts/{script_id}"),
        "script_queue" to ContractEndpoint("POST", "/api/scripts/{script_id}/queue"),
        "script_run_now" to ContractEndpoint("POST", "/api/scripts/{script_id}/run-now"),
        "queue_get" to ContractEndpoint("GET", "/api/queue"),
        "queue_clear" to ContractEndpoint("DELETE", "/api/queue"),
        "queue_play" to ContractEndpoint("POST", "/api/queue/play"),
        "queue_pause" to ContractEndpoint("POST", "/api/queue/pause"),
        "queue_stop" to ContractEndpoint("POST", "/api/queue/stop"),
        "queue_item_delete" to ContractEndpoint("DELETE", "/api/queue/{queue_id}"),
        "queue_reorder" to ContractEndpoint("PUT", "/api/queue/reorder"),
        "queue_settings" to ContractEndpoint("PUT", "/api/queue/settings"),
        "queue_item_patch" to ContractEndpoint("PATCH", "/api/queue/{queue_id}"),
        "type_to_talk_get" to ContractEndpoint("GET", "/api/type-to-talk"),
        "type_to_talk_add" to ContractEndpoint("POST", "/api/type-to-talk"),
        "type_to_talk_clear" to ContractEndpoint("DELETE", "/api/type-to-talk"),
        "type_to_talk_settings" to ContractEndpoint("PATCH", "/api/type-to-talk/settings"),
        "type_to_talk_play" to ContractEndpoint("POST", "/api/type-to-talk/play"),
        "type_to_talk_stop" to ContractEndpoint("POST", "/api/type-to-talk/stop"),
        "type_to_talk_item_delete" to ContractEndpoint("DELETE", "/api/type-to-talk/{item_id}"),
        "type_to_talk_reorder" to ContractEndpoint("PUT", "/api/type-to-talk/reorder"),
        "plugins_get" to ContractEndpoint("GET", "/api/plugins"),
        "plugin_set" to ContractEndpoint("PUT", "/api/plugins/{plugin_id}"),
        "plugins_reload" to ContractEndpoint("POST", "/api/plugins/reload"),
        "plugin_reload" to ContractEndpoint("POST", "/api/plugins/{plugin_id}/reload"),
        "plugin_recover" to ContractEndpoint("POST", "/api/plugins/{plugin_id}/recover"),
        "plugins_reset_metrics" to ContractEndpoint("POST", "/api/plugins/reset-metrics"),
        "plugin_reset_metrics" to ContractEndpoint("POST", "/api/plugins/{plugin_id}/reset-metrics"),
        "models_list" to ContractEndpoint("GET", "/api/models"),
        "model_pull" to ContractEndpoint("POST", "/api/models/pull/{model:path}"),
        "ai_restart" to ContractEndpoint("POST", "/api/ai/restart-engine"),
        "ai_reload_asr" to ContractEndpoint("POST", "/api/ai/reload-asr"),
        "ai_reload_kokoro" to ContractEndpoint("POST", "/api/ai/reload-kokoro"),
        "audio_devices" to ContractEndpoint("GET", "/api/audio/devices"),
        "audio_refresh" to ContractEndpoint("POST", "/api/audio/refresh"),
        "audio_restart" to ContractEndpoint("POST", "/api/audio/restart-engine"),
        "audio_test_input" to ContractEndpoint("POST", "/api/audio/test-input"),
        "audio_test_output" to ContractEndpoint("POST", "/api/audio/test-output"),
        "audio_test_duplex_lock" to ContractEndpoint("POST", "/api/audio/test-duplex-lock"),
        "audio_library_get" to ContractEndpoint("GET", "/api/audio-library"),
        "audio_library_upload" to ContractEndpoint("POST", "/api/audio-library/upload"),
        "audio_library_play" to ContractEndpoint("POST", "/api/audio-library/{name:path}/play"),
        "audio_library_stop" to ContractEndpoint("POST", "/api/audio-library/stop"),
        "audio_library_rename" to ContractEndpoint("PATCH", "/api/audio-library/{name:path}"),
        "audio_library_delete" to ContractEndpoint("DELETE", "/api/audio-library/{name:path}"),
        "diagnostics_get" to ContractEndpoint("GET", "/api/diagnostics"),
        "diagnostics_self_test" to ContractEndpoint("POST", "/api/diagnostics/self-test"),
        "diagnostics_logs_clear" to ContractEndpoint("DELETE", "/api/diagnostics/logs"),
        "diagnostics_turns_clear" to ContractEndpoint("DELETE", "/api/diagnostics/turns"),
        "diagnostics_export" to ContractEndpoint("GET", "/api/diagnostics/export"),
        "backup_status" to ContractEndpoint("GET", "/api/backup/status"),
        "backup_download" to ContractEndpoint("GET", "/api/backup"),
        "backup_restore" to ContractEndpoint("POST", "/api/restore"),
        "configuration_options" to ContractEndpoint("GET", "/api/configuration-options"),
        "tts_edge_voices" to ContractEndpoint("GET", "/api/tts/edge-voices"),
    )

    val requiredRequestFields: Map<String, Set<String>> = mapOf(
        "auth_login" to setOf("pin", "client_name", "client_type", "client_version", "api_version"),
        "auth_device_login" to setOf("device_id", "device_token", "client_name", "client_type", "client_version", "api_version"),
        "pairing_start" to setOf("preferred_server_url"),
        "pairing_claim" to setOf("pairing_id", "secret", "short_code", "device_name", "device_type", "device_version", "platform"),
        "knowledge_agent_libraries_set" to setOf("library_ids"),
    )

    val requiredResponseFields: Map<String, Set<String>> = mapOf(
        "auth_grant" to setOf("token", "server_version", "api_version", "websocket_protocol_version", "heartbeat_interval_seconds", "heartbeat_timeout_seconds", "session"),
        "ws_ticket" to setOf("ticket"),
        "pairing_start" to setOf("pairing_id", "pairing_uri"),
        "pairing_claim" to setOf("device_id", "device_token"),
        "bootstrap" to setOf("agents", "messages", "mode"),
        "devices" to setOf("devices"),
    )

    private fun pathRegex(template: String): Regex {
        val out = StringBuilder("^")
        var index = 0
        while (index < template.length) {
            if (template[index] == '{') {
                val end = template.indexOf('}', index)
                require(end > index) { "Invalid contract path template: $template" }
                val token = template.substring(index + 1, end)
                out.append(if (token.endsWith(":path")) ".+" else "[^/]+")
                index = end + 1
            } else {
                out.append(Regex.escape(template[index].toString()))
                index++
            }
        }
        return Regex(out.append('$').toString())
    }

    fun endpoint(name: String): ContractEndpoint =
        endpoints[name] ?: error("Unknown Android Core contract operation: $name")

    fun matches(method: String, requestPath: String): Boolean {
        val path = requestPath.substringBefore('?')
        return endpoints.values.any { spec ->
            spec.method == method.uppercase() && pathRegex(spec.path).matches(path)
        }
    }

    fun requireDeclared(method: String, requestPath: String) {
        if (!matches(method, requestPath)) {
            throw IllegalArgumentException("Android request is outside the declared VerbaNode mobile contract: ${method.uppercase()} $requestPath")
        }
    }

    fun validate(remote: MobileContract) {
        if (remote.contractVersion != CONTRACT_VERSION) protocolError("/api/client-info.mobile_contract", "unsupported mobile contract version ${remote.contractVersion}")
        if (!(remote.minimumApiVersion <= API_VERSION && API_VERSION <= remote.apiVersion)) {
            protocolError("/api/client-info.mobile_contract", "Android API $API_VERSION is outside server range ${remote.minimumApiVersion}..${remote.apiVersion}")
        }
        if (remote.websocketProtocolVersion != WEBSOCKET_PROTOCOL_VERSION) protocolError("/api/client-info.mobile_contract", "unsupported WebSocket protocol ${remote.websocketProtocolVersion}")
        if (remote.sessionHeader != SESSION_HEADER) protocolError("/api/client-info.mobile_contract", "session header changed to '${remote.sessionHeader}'")
        if (remote.websocketEndpoint != WEBSOCKET_ENDPOINT) protocolError("/api/client-info.mobile_contract", "WebSocket endpoint changed to '${remote.websocketEndpoint}'")
        if (remote.websocketTicketEndpoint != WEBSOCKET_TICKET_ENDPOINT) protocolError("/api/client-info.mobile_contract", "WebSocket ticket endpoint changed to '${remote.websocketTicketEndpoint}'")

        endpoints.forEach { (name, expected) ->
            val actual = remote.endpoints[name] ?: protocolError("/api/client-info.mobile_contract", "missing endpoint '$name'")
            if (actual != expected) protocolError("/api/client-info.mobile_contract", "endpoint '$name' changed from ${expected.method} ${expected.path} to ${actual.method} ${actual.path}")
        }
        requiredRequestFields.forEach { (name, expected) ->
            val actual = remote.requestFields[name] ?: protocolError("/api/client-info.mobile_contract", "missing request field contract '$name'")
            if (!actual.containsAll(expected)) protocolError("/api/client-info.mobile_contract", "request fields for '$name' no longer support ${expected - actual}")
        }
        requiredResponseFields.forEach { (name, expected) ->
            val actual = remote.responseFields[name] ?: protocolError("/api/client-info.mobile_contract", "missing response field contract '$name'")
            if (!actual.containsAll(expected)) protocolError("/api/client-info.mobile_contract", "response fields for '$name' no longer guarantee ${expected - actual}")
        }
        val expectedCloseCodes = mapOf(
            "unauthorized" to WS_CLOSE_UNAUTHORIZED,
            "origin_rejected" to WS_CLOSE_ORIGIN_REJECTED,
            "protocol_unsupported" to WS_CLOSE_PROTOCOL_UNSUPPORTED,
            "heartbeat_timeout" to WS_CLOSE_HEARTBEAT_TIMEOUT,
        )
        expectedCloseCodes.forEach { (name, expected) ->
            if (remote.websocketCloseCodes[name] != expected) protocolError("/api/client-info.mobile_contract", "WebSocket close code '$name' changed")
        }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("contract_version", CONTRACT_VERSION)
        .put("api_version", API_VERSION)
        .put("minimum_api_version", API_VERSION)
        .put("websocket_protocol_version", WEBSOCKET_PROTOCOL_VERSION)
        .put("session_header", SESSION_HEADER)
        .put("websocket_endpoint", WEBSOCKET_ENDPOINT)
        .put("websocket_ticket_endpoint", WEBSOCKET_TICKET_ENDPOINT)
        .put("endpoints", JSONObject().apply { endpoints.forEach { (name, spec) -> put(name, JSONObject().put("method", spec.method).put("path", spec.path)) } })
        .put("request_fields", JSONObject().apply { requiredRequestFields.forEach { (name, fields) -> put(name, JSONArray(fields.toList())) } })
        .put("response_fields", JSONObject().apply { requiredResponseFields.forEach { (name, fields) -> put(name, JSONArray(fields.toList())) } })
        .put("websocket_close_codes", JSONObject()
            .put("unauthorized", WS_CLOSE_UNAUTHORIZED)
            .put("origin_rejected", WS_CLOSE_ORIGIN_REJECTED)
            .put("protocol_unsupported", WS_CLOSE_PROTOCOL_UNSUPPORTED)
            .put("heartbeat_timeout", WS_CLOSE_HEARTBEAT_TIMEOUT))
}

private fun JSONObject.stringSetMap(field: String, context: String): Map<String, Set<String>> {
    val parent = requireObject(field, context)
    return buildMap {
        val keys = parent.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val array = parent.requireArray(key, "$context.$field")
            val values = buildSet {
                for (index in 0 until array.length()) {
                    val value = array.opt(index)
                    if (value !is String || value.isBlank()) protocolError("$context.$field.$key", "array item $index must be a non-blank string")
                    add(value)
                }
            }
            put(key, values)
        }
    }
}

internal fun parseMobileContract(json: JSONObject): MobileContract {
    val context = "/api/client-info.mobile_contract"
    val endpointsJson = json.requireObject("endpoints", context)
    val endpoints = buildMap {
        val keys = endpointsJson.keys()
        while (keys.hasNext()) {
            val name = keys.next()
            val spec = endpointsJson.requireObject(name, "$context.endpoints")
            put(name, ContractEndpoint(spec.requireString("method", "$context.endpoints.$name").uppercase(), spec.requireString("path", "$context.endpoints.$name")))
        }
    }
    val closeCodesJson = json.requireObject("websocket_close_codes", context)
    val closeCodes = buildMap {
        val keys = closeCodesJson.keys()
        while (keys.hasNext()) {
            val name = keys.next()
            put(name, closeCodesJson.requireInt(name, "$context.websocket_close_codes"))
        }
    }
    return MobileContract(
        contractVersion = json.requireInt("contract_version", context),
        apiVersion = json.requireInt("api_version", context),
        minimumApiVersion = json.requireInt("minimum_api_version", context),
        websocketProtocolVersion = json.requireInt("websocket_protocol_version", context),
        sessionHeader = json.requireString("session_header", context),
        websocketEndpoint = json.requireString("websocket_endpoint", context),
        websocketTicketEndpoint = json.requireString("websocket_ticket_endpoint", context),
        endpoints = endpoints,
        requestFields = json.stringSetMap("request_fields", context),
        responseFields = json.stringSetMap("response_fields", context),
        websocketCloseCodes = closeCodes,
    )
}
