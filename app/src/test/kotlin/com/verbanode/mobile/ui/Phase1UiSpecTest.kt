package com.verbanode.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class Phase1UiSpecTest {
    @Test
    fun connectionAndPairingTabsMatchApprovedOutlook() {
        assertEquals(listOf("Saved", "Wi-Fi Scan", "Manual"), Phase1UiSpec.connectionTabs)
        assertEquals(listOf("Enter Code", "Scan QR"), Phase1UiSpec.pairingTabs)
    }

    @Test
    fun homeCardsMatchApprovedOutlookOrder() {
        assertEquals(
            listOf(
                "Agents",
                "Knowledge",
                "Chat",
                "Type to Talk",
                "Push to Talk",
                "Scripts",
                "Audio",
                "Backups",
                "Diagnostics",
            ),
            Phase1UiSpec.homeCards.map { it.title },
        )
        assertEquals(
            listOf(
                "Manage agents",
                "Manage knowledge",
                "Text conversation",
                "Text to speech",
                "Voice with STT",
                "TTS library",
                "Manage audio",
                "Save and restore",
                "System health",
            ),
            Phase1UiSpec.homeCards.map { it.subtitle },
        )
    }
}
