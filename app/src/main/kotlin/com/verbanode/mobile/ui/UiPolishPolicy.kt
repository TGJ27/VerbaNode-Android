package com.verbanode.mobile.ui

internal object UiPolishPolicy {
    val scriptTransportLabels: List<String> = listOf("Play", "Pause", "Stop", "Loop")
    const val immersiveSystemBars: Boolean = true
    const val transientBarsBySwipe: Boolean = true

    fun agentActionRows(active: Boolean): List<List<String>> = if (active) {
        listOf(listOf("Clear memory", "Backup"), listOf("Delete"))
    } else {
        listOf(listOf("Activate", "Clear memory"), listOf("Backup", "Delete"))
    }
}
