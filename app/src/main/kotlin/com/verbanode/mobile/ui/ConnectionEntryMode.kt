package com.verbanode.mobile.ui

enum class ConnectionEntryMode(val label: String, val usesDiscovery: Boolean = false) {
    SAVED("Saved"),
    SCAN("Wi-Fi Scan", usesDiscovery = true),
    MANUAL("Manual"),
}
