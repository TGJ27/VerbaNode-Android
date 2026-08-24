package com.verbanode.mobile

import com.verbanode.mobile.network.ApiException
import com.verbanode.mobile.network.ApiProtocolException
import org.json.JSONObject

internal fun friendlyError(error: Exception): String = when (error) {
    is ApiException, is ApiProtocolException -> error.message ?: "VerbaNode request failed"
    else -> error.message ?: error.javaClass.simpleName
}

internal fun pipelineStatusLabel(stage: String, mode: String): String = when (stage.lowercase()) {
    "starting" -> "Starting"
    "idle" -> if (mode == "conversation") "Listening" else "Ready"
    "listening" -> "Listening"
    "recording" -> "Recording"
    "transcribing" -> "Transcribing"
    "thinking" -> "Generating"
    "tooling" -> "Running tool"
    "speaking" -> "Speaking"
    "recovering" -> "Recovering"
    "error" -> "Error"
    "stopped" -> "Ready"
    else -> stage.replace('_', ' ').replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}

internal fun modeStatusLabel(mode: String): String = when (mode) {
    "conversation" -> "Listening"
    "ptt", "browser_ptt" -> "Recording"
    "processing" -> "Transcribing"
    else -> "Ready"
}

internal fun prettyStatus(status: JSONObject): String {
    val agent = status.optJSONObject("active_agent")?.optString("name", "Unknown") ?: "Unknown"
    val audio = status.optJSONObject("audio") ?: JSONObject()
    val ai = status.optJSONObject("ai") ?: JSONObject()
    return buildString {
        appendLine("Mode: ${status.optString("mode", "unknown")}")
        appendLine("Agent: $agent")
        appendLine("AI: ${ai.optString("mode", "unknown")}")
        appendLine("Audio: ${audio.optString("mode", "unknown")}")
        appendLine("Connected controller: ${status.optJSONObject("controller")?.optString("client_name", "unknown")}")
    }.trim()
}
