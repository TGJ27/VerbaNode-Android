package com.verbanode.mobile.agent

val DEFAULT_AGENT_TOOL_IDS: Set<String> = setOf(
    "get_current_time",
    "get_location",
    "get_weather",
    "handle_exit_intent",
)

data class AgentToolOption(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val status: String,
)

data class AgentDraft(
    val name: String,
    val avatar: String,
    val color: String,
    val role: String,
    val systemPrompt: String,
    val greeting: String,
    val llmModel: String,
    val language: String,
    val ttsMode: String,
    val edgeVoice: String,
    val kokoroVoiceId: Int,
    val ttsRate: Double,
    val ttsVolume: Double,
    val sttModel: String,
    val temperature: Double,
    val topP: Double,
    val maxTokens: Int,
    val contextSize: Int,
    val toolsEnabled: Set<String>,
    val knowledgeLibraryIds: Set<Int>,
)

fun normalizeAgentDraft(value: AgentDraft): AgentDraft = value.copy(
    name = value.name.trim().take(80),
    avatar = value.avatar.trim().take(8).ifBlank { "AI" },
    color = value.color.trim().ifBlank { "#3578f6" },
    role = value.role.trim(),
    systemPrompt = value.systemPrompt.trim(),
    greeting = value.greeting.trim(),
    llmModel = value.llmModel.trim(),
    language = if (value.language == "id") "id" else "en",
    ttsMode = value.ttsMode.trim().ifBlank { "edge_fallback" },
    edgeVoice = value.edgeVoice.trim(),
    kokoroVoiceId = value.kokoroVoiceId.coerceIn(0, 102),
    ttsRate = value.ttsRate.coerceIn(0.5, 2.0),
    ttsVolume = value.ttsVolume.coerceIn(0.0, 1.0),
    sttModel = value.sttModel.trim(),
    temperature = value.temperature.coerceIn(0.0, 2.0),
    topP = value.topP.coerceIn(0.0, 1.0),
    maxTokens = value.maxTokens.coerceIn(32, 8192),
    contextSize = value.contextSize.coerceIn(512, 131072),
    toolsEnabled = value.toolsEnabled.map(String::trim).filter(String::isNotBlank).toSortedSet(),
    knowledgeLibraryIds = value.knowledgeLibraryIds.filter { it > 0 }.toSortedSet(),
)

fun agentMatchesQuery(name: String, role: String, llmModel: String, query: String): Boolean {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return true
    return sequenceOf(name, role, llmModel).any { it.lowercase().contains(normalized) }
}

fun agentToolOptions(values: List<AgentToolOption>): List<AgentToolOption> = values
    .filterNot { it.status.equals("load_error", ignoreCase = true) }
    .sortedWith(compareBy<AgentToolOption> { it.name.lowercase() }.thenBy { it.id })
