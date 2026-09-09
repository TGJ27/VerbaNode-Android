package com.verbanode.mobile.ui

internal object Phase3UiSpec {
    val managementScreens = listOf("Agents", "Agent Details", "Knowledge", "Scripts", "Audio", "Backups / Restore")

    fun chatStatusLabel(connected: Boolean, chatStatus: String): String {
        if (!connected) return "Offline"
        return when (chatStatus.trim().lowercase()) {
            "recording", "listening" -> "Listening"
            "transcribing", "transcription" -> "Transcribing"
            "generating", "thinking", "waiting", "processing", "sending", "uploading" -> "Thinking"
            "speaking", "preparing speech", "tts" -> "Speaking"
            else -> "Ready"
        }
    }
}
