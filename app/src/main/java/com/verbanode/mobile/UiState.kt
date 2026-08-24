package com.verbanode.mobile

import com.verbanode.mobile.discovery.DiscoveredServer
import com.verbanode.mobile.network.Agent
import com.verbanode.mobile.network.AudioLibraryItem
import com.verbanode.mobile.network.AuthSession
import com.verbanode.mobile.network.ChatMessage
import com.verbanode.mobile.network.ClientInfo
import com.verbanode.mobile.network.ConnectionState
import com.verbanode.mobile.network.ProbeResult
import com.verbanode.mobile.network.ScriptItem
import com.verbanode.mobile.network.ScriptQueueItem
import com.verbanode.mobile.network.TrustedDevice
import com.verbanode.mobile.network.TypeToTalkItem
import com.verbanode.mobile.storage.ServerProfile
import org.json.JSONObject

enum class AppScreen {
    SERVERS, TRUST, LOGIN,
    HOME, CHAT, AGENTS, MORE, INFORMATION, SCRIPTS, AUDIO, TYPE_TO_TALK, PLUGINS, SETTINGS,
    DEVICES, DIAGNOSTICS, DATA, STATUS
}

data class MobileUiState(
    val screen: AppScreen = AppScreen.SERVERS,
    val profiles: List<ServerProfile> = emptyList(),
    val discovered: List<DiscoveredServer> = emptyList(),
    val discoveryActive: Boolean = false,
    val currentProfile: ServerProfile? = null,
    val trustCandidate: ProbeResult? = null,
    val clientInfo: ClientInfo? = null,
    val session: AuthSession? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val connectionLabel: String = "Disconnected",
    val agents: List<Agent> = emptyList(),
    val activeAgent: Agent? = null,
    val conversationId: Int? = null,
    val conversationActive: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val mode: String = "idle",
    val devices: List<TrustedDevice> = emptyList(),
    val pairingStatus: JSONObject? = null,
    val rawAgents: List<JSONObject> = emptyList(),
    val informationItems: List<JSONObject> = emptyList(),
    val scriptItems: List<ScriptItem> = emptyList(),
    val queueItems: List<ScriptQueueItem> = emptyList(),
    val queueState: String = "paused",
    val queueLoop: Boolean = false,
    val configurationOptions: JSONObject? = null,
    val scriptDefaults: JSONObject? = null,
    val typeToTalkItems: List<TypeToTalkItem> = emptyList(),
    val typeToTalkState: String = "idle",
    val typeToTalkSettings: JSONObject? = null,
    val audioLibraryItems: List<AudioLibraryItem> = emptyList(),
    val audioLibraryPlaying: String? = null,
    val chatAutoScroll: Boolean = true,
    val pluginItems: List<JSONObject> = emptyList(),
    val pluginSummary: JSONObject? = null,
    val modelItems: List<JSONObject> = emptyList(),
    val runtimeSettings: JSONObject? = null,
    val audioDevices: JSONObject? = null,
    val dashboardStatus: JSONObject? = null,
    val pipelineStatus: JSONObject? = null,
    val capabilityStatus: JSONObject? = null,
    val diagnosticsStatus: JSONObject? = null,
    val backupStatus: JSONObject? = null,
    val statusText: String = "",
    val chatStatus: String = "Ready",
    val activeOperations: Set<String> = emptySet(),
    val recording: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
) {
    val connected: Boolean get() = connectionState == ConnectionState.CONNECTED
    val busy: Boolean get() = activeOperations.isNotEmpty()
}
