package com.verbanode.mobile.ui

internal object Phase2UiSpec {
    val typeToTalkSettings = listOf("TTS / Voice")
    const val typeToTalkHasModel = false
    const val typeToTalkHasAgent = false
    const val typeToTalkHasListeningOrTranscription = false
    val pushToTalkStates = listOf("Ready", "Listening", "Transcribing", "Sending", "Waiting", "Speaking")

    fun pushToTalkVisualState(recording: Boolean, chatStatus: String): String {
        if (recording) return "Listening"
        return when (chatStatus.trim().lowercase()) {
            "recording", "listening" -> "Listening"
            "transcribing", "transcription" -> "Transcribing"
            "sending", "uploading" -> "Sending"
            "generating", "thinking", "waiting", "processing" -> "Waiting"
            "speaking", "preparing speech", "tts" -> "Speaking"
            else -> "Ready"
        }
    }

    const val chatHasAttachment = false
}
