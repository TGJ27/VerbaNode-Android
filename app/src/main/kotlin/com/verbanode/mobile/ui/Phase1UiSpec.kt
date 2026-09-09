package com.verbanode.mobile.ui

internal data class HomeCardSpec(
    val title: String,
    val subtitle: String,
)

internal object Phase1UiSpec {
    val connectionTabs = listOf("Saved", "Wi-Fi Scan", "Manual")
    val pairingTabs = listOf("Enter Code", "Scan QR")
    val homeCards = listOf(
        HomeCardSpec("Agents", "Manage agents"),
        HomeCardSpec("Knowledge", "Manage knowledge"),
        HomeCardSpec("Chat", "Text conversation"),
        HomeCardSpec("Type to Talk", "Text to speech"),
        HomeCardSpec("Push to Talk", "Voice with STT"),
        HomeCardSpec("Scripts", "TTS library"),
        HomeCardSpec("Audio", "Manage audio"),
        HomeCardSpec("Backups", "Save and restore"),
        HomeCardSpec("Diagnostics", "System health"),
    )
}
