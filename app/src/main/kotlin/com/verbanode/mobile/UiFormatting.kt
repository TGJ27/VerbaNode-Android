package com.verbanode.mobile

internal fun friendlyError(error: Throwable): String =
    error.message ?: error.javaClass.simpleName

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
