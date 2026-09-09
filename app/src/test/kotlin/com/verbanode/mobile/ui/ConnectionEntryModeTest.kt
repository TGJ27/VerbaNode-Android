package com.verbanode.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionEntryModeTest {
    @Test
    fun modesHaveStableLabels() {
        assertEquals(listOf("Saved", "Wi-Fi Scan", "Manual"), ConnectionEntryMode.entries.map { it.label })
    }

    @Test
    fun scanModeIsTheOnlyDiscoveryMode() {
        assertEquals(ConnectionEntryMode.SCAN, ConnectionEntryMode.entries.single { it.usesDiscovery })
    }
}
