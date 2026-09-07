package com.verbanode.mobile

import com.verbanode.mobile.discovery.DiscoveredServer
import com.verbanode.mobile.network.Agent
import com.verbanode.mobile.network.AuthSession
import com.verbanode.mobile.network.ChatMessage
import com.verbanode.mobile.network.ClientInfo
import com.verbanode.mobile.network.ProbeResult
import com.verbanode.mobile.network.TrustedDevice
import com.verbanode.mobile.storage.ServerProfile
import org.json.JSONObject

enum class AppScreen {
    SERVERS, TRUST, LOGIN,
    HOME, CHAT, AGENTS, MORE, KNOWLEDGE, SCRIPTS, AUDIO, TYPE_TO_TALK, PLUGINS, SETTINGS,
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
    val connected: Boolean = false,
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
    val knowledgeStatus: JSONObject? = null,
    val knowledgeLibraries: List<JSONObject> = emptyList(),
    val knowledgeDocuments: List<JSONObject> = emptyList(),
    val knowledgeAllDocuments: List<JSONObject> = emptyList(),
    val knowledgeLoading: Boolean = false,
    val knowledgeLoadError: String? = null,
    val selectedKnowledgeLibraryId: Int? = null,
    val knowledgeSearchResult: JSONObject? = null,
    val knowledgeDocumentContent: JSONObject? = null,
    val scriptItems: List<JSONObject> = emptyList(),
    val queueItems: List<JSONObject> = emptyList(),
    val queueState: String = "paused",
    val queueLoop: Boolean = false,
    val configurationOptions: JSONObject? = null,
    val scriptDefaults: JSONObject? = null,
    val typeToTalkItems: List<JSONObject> = emptyList(),
    val typeToTalkState: String = "idle",
    val typeToTalkSettings: JSONObject? = null,
    val audioLibraryItems: List<JSONObject> = emptyList(),
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
    val busy: Boolean = false,
    val recording: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)
