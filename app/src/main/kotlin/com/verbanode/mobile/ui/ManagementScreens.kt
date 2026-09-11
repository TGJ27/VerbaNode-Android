package com.verbanode.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.verbanode.mobile.AppScreen
import com.verbanode.mobile.AppViewModel
import com.verbanode.mobile.MainActivity
import com.verbanode.mobile.agent.AgentDraft
import com.verbanode.mobile.agent.AgentToolOption
import com.verbanode.mobile.agent.DEFAULT_AGENT_TOOL_IDS
import com.verbanode.mobile.agent.agentMatchesQuery
import com.verbanode.mobile.agent.agentToolOptions
import com.verbanode.mobile.agent.normalizeAgentDraft
import com.verbanode.mobile.knowledge.KnowledgeDocumentRef
import com.verbanode.mobile.knowledge.KnowledgeDocumentScope
import com.verbanode.mobile.knowledge.KnowledgeIngestionJobRef
import com.verbanode.mobile.knowledge.KnowledgeSourceFilter
import com.verbanode.mobile.knowledge.KnowledgeStatusFilter
import com.verbanode.mobile.knowledge.knowledgeJobProgressPercent
import com.verbanode.mobile.knowledge.knowledgeMatchesQuery
import com.verbanode.mobile.knowledge.knowledgeMatchesScope
import com.verbanode.mobile.knowledge.knowledgeMatchesSourceFilter
import com.verbanode.mobile.knowledge.knowledgeMatchesStatus
import com.verbanode.mobile.knowledge.knowledgeOverviewCounts
import com.verbanode.mobile.knowledge.knowledgeSourceLabel
import com.verbanode.mobile.knowledge.latestKnowledgeJob
import org.json.JSONArray
import org.json.JSONObject

private fun choiceArray(array: JSONArray?): List<Pair<String, String>> = buildList {
    if (array == null) return@buildList
    for (index in 0 until array.length()) {
        when (val value = array.opt(index)) {
            is JSONObject -> {
                val key = value.optString("value")
                if (key.isNotBlank()) add(key to value.optString("label", key))
            }
            is String -> if (value.isNotBlank()) add(value to value)
        }
    }
}

private fun configChoices(config: JSONObject?, key: String): List<Pair<String, String>> = choiceArray(config?.optJSONArray(key))
private fun sttChoices(config: JSONObject?, language: String): List<Pair<String, String>> =
    choiceArray(config?.optJSONObject("stt_models")?.optJSONArray(language))

private fun edgeVoiceChoices(config: JSONObject?, language: String): List<Pair<String, String>> {
    val array = config?.optJSONArray("edge_voices") ?: JSONArray()
    val prefix = if (language == "id") "id-" else "en-"
    return buildList {
        for (index in 0 until array.length()) {
            val voice = array.optJSONObject(index) ?: continue
            val shortName = voice.optString("short_name").trim()
            if (shortName.isBlank() || !shortName.lowercase().startsWith(prefix)) continue
            val name = voice.optString("name", shortName).trim().ifBlank { shortName }
            val locale = voice.optString("locale").trim()
            val gender = voice.optString("gender").trim()
            val details = listOf(locale, gender).filter { it.isNotBlank() }.joinToString(" · ")
            add(shortName to if (details.isBlank()) name else "$name · $details")
        }
    }
}

@Composable
internal fun AgentsScreen(viewModel: AppViewModel, activity: MainActivity) {
    val state by viewModel.ui.collectAsState()
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<JSONObject?>(null) }
    var memoryTarget by remember { mutableStateOf<JSONObject?>(null) }
    var query by remember { mutableStateOf("") }
    var activeOnly by remember { mutableStateOf(false) }

    val activeId = state.activeAgent?.id
    val visibleAgents = state.rawAgents.filter { agent ->
        (!activeOnly || agent.optInt("id") == activeId) && agentMatchesQuery(
            agent.optString("name"),
            agent.optString("role"),
            agent.optString("llm_model"),
            query,
        )
    }
    val reportedTools = agentToolOptions(state.pluginItems.mapNotNull { plugin ->
        val id = plugin.optString("id").trim()
        if (id.isBlank()) null else AgentToolOption(
            id = id,
            name = plugin.optString("name", id).ifBlank { id },
            enabled = plugin.optBoolean("enabled", true),
            status = plugin.optString("status", "healthy"),
        )
    })
    val toolOptions = if (reportedTools.isNotEmpty()) reportedTools else DEFAULT_AGENT_TOOL_IDS.sorted().map { id ->
        AgentToolOption(id, id.replace('_', ' ').replaceFirstChar(Char::uppercaseChar), true, "fallback")
    }

    ManagementScaffold(viewModel, "Agents", AppScreen.AGENTS) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Feedback(viewModel) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(120) },
                        label = { Text("Search agents") },
                        placeholder = { Text("Name, role, or model") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedButton(onClick = viewModel::refreshAgents, enabled = !state.agentsLoading) { Text("↻") }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = !activeOnly, onClick = { activeOnly = false }, label = { Text("All") }) }
                    item { FilterChip(selected = activeOnly, onClick = { activeOnly = true }, label = { Text("Active") }) }
                }
            }
            if (state.agentsLoading && state.rawAgents.isEmpty()) {
                item { DashboardCard("Loading agents", "Reading agent configuration from Core…") { Text("Please wait.") } }
            }
            state.agentsLoadError?.let { message ->
                item { DashboardCard("Agent refresh failed", message) { OutlinedButton(onClick = viewModel::refreshAgents) { Text("Retry") } } }
            }
            if (!state.agentsLoading && state.rawAgents.isEmpty() && state.agentsLoadError == null) {
                item { DashboardCard("No agents", "Create an agent to begin.") { Button(onClick = { creating = true }) { Text("Create agent") } } }
            } else if (visibleAgents.isEmpty() && state.rawAgents.isNotEmpty()) {
                item { DashboardCard("No matching agents", "Change the search or active filter.") { OutlinedButton(onClick = { query = ""; activeOnly = false }) { Text("Clear filters") } } }
            }
            items(visibleAgents, key = { it.optInt("id") }) { agent ->
                val id = agent.optInt("id")
                val active = activeId == id
                val knowledgeCount = agent.optJSONArray("knowledge_library_ids")?.length() ?: 0
                val toolCount = agent.optJSONArray("tools_enabled")?.length() ?: 0
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (active) "●" else "○", color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(agent.optString("name", "Agent"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (active) Pill("ACTIVE")
                                }
                                Text(
                                    agent.optString("role"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            OutlinedButton(onClick = { editing = agent }) { Text("Details") }
                        }
                        if (active) {
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { memoryTarget = agent }, modifier = Modifier.weight(1f)) { Text("Clear memory", maxLines = 1) }
                                OutlinedButton(
                                    onClick = { viewModel.exportAgent(id) { bytes, name, mime -> activity.saveDocument(bytes, name, mime) } },
                                    modifier = Modifier.weight(1f),
                                ) { Text("Backup", maxLines = 1) }
                            }
                            OutlinedButton(
                                onClick = { deleteTarget = agent },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) { Text("Delete", maxLines = 1, color = MaterialTheme.colorScheme.error) }
                        } else {
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.selectAgent(id) }, modifier = Modifier.weight(1f)) { Text("Activate", maxLines = 1) }
                                OutlinedButton(onClick = { memoryTarget = agent }, modifier = Modifier.weight(1f)) { Text("Clear memory", maxLines = 1) }
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.exportAgent(id) { bytes, name, mime -> activity.saveDocument(bytes, name, mime) } },
                                    modifier = Modifier.weight(1f),
                                ) { Text("Backup", maxLines = 1) }
                                OutlinedButton(onClick = { deleteTarget = agent }, modifier = Modifier.weight(1f)) {
                                    Text("Delete", maxLines = 1, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Create Agent") }
            }
        }
    }
    if (creating || editing != null) {
        AgentEditorDialog(
            existing = editing,
            configurationOptions = state.configurationOptions,
            knowledgeLibraries = state.knowledgeLibraries,
            toolOptions = toolOptions,
            onDismiss = { creating = false; editing = null },
            onGenerateRole = viewModel::generateAgentRole,
        ) { id, payload ->
            creating = false
            editing = null
            viewModel.saveAgent(id, payload)
        }
    }
    memoryTarget?.let { agent ->
        ConfirmDialog(
            title = "Clear ${agent.optString("name", "agent")} memory?",
            message = "This permanently clears this agent's saved conversations and summaries. The agent configuration and Knowledge assignments are kept.",
            onDismiss = { memoryTarget = null },
            onConfirm = { val id = agent.optInt("id"); memoryTarget = null; viewModel.clearAgentMemoryManagement(id) },
        )
    }
    deleteTarget?.let { agent ->
        ConfirmDialog(
            title = "Delete ${agent.optString("name", "agent")}?",
            message = "This permanently removes the agent and its conversation history. Knowledge libraries themselves are not deleted. Core requires at least one agent to remain.",
            onDismiss = { deleteTarget = null },
            onConfirm = { val id = agent.optInt("id"); deleteTarget = null; viewModel.deleteAgentManagement(id) },
        )
    }
}

@Composable
private fun AgentEditorDialog(
    existing: JSONObject?,
    configurationOptions: JSONObject?,
    knowledgeLibraries: List<JSONObject>,
    toolOptions: List<AgentToolOption>,
    onDismiss: () -> Unit,
    onGenerateRole: (String, String?, (JSONObject) -> Unit) -> Unit,
    onSave: (Int?, JSONObject) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.optString("name", "Ropi") ?: "Ropi") }
    var avatar by remember(existing) { mutableStateOf(existing?.optString("avatar", "RP") ?: "RP") }
    var color by remember(existing) { mutableStateOf(existing?.optString("color", "#3578f6") ?: "#3578f6") }
    var role by remember(existing) { mutableStateOf(existing?.optString("role", "Helpful assistant") ?: "Helpful assistant") }
    var systemPrompt by remember(existing) { mutableStateOf(existing?.optString("system_prompt", "You are a helpful assistant.") ?: "You are a helpful assistant.") }
    var greeting by remember(existing) { mutableStateOf(existing?.optString("greeting", "Hello. How can I help?") ?: "Hello. How can I help?") }
    var roleDescription by remember(existing) { mutableStateOf("") }
    var model by remember(existing) { mutableStateOf(existing?.optString("llm_model", "qwen3.5:0.8b") ?: "qwen3.5:0.8b") }
    var language by remember(existing) { mutableStateOf(existing?.optString("language", "en") ?: "en") }
    var ttsMode by remember(existing) { mutableStateOf(existing?.optString("tts_mode", "edge_fallback") ?: "edge_fallback") }
    var edgeVoice by remember(existing) { mutableStateOf(existing?.optString("edge_voice", "en-US-AriaNeural") ?: "en-US-AriaNeural") }
    var kokoroVoiceId by remember(existing) { mutableStateOf((existing?.optInt("kokoro_voice_id", 0) ?: 0).toString()) }
    var ttsRate by remember(existing) { mutableStateOf((existing?.optDouble("tts_rate", 1.0) ?: 1.0).toString()) }
    var ttsVolume by remember(existing) { mutableStateOf((existing?.optDouble("tts_volume", 1.0) ?: 1.0).toString()) }
    var sttModel by remember(existing) { mutableStateOf(existing?.optString("stt_model", "iic/SenseVoiceSmall") ?: "iic/SenseVoiceSmall") }
    var temperature by remember(existing) { mutableStateOf((existing?.optDouble("temperature", 0.6) ?: 0.6).toString()) }
    var topP by remember(existing) { mutableStateOf((existing?.optDouble("top_p", 0.9) ?: 0.9).toString()) }
    var maxTokens by remember(existing) { mutableStateOf((existing?.optInt("max_tokens", 1024) ?: 1024).toString()) }
    var contextSize by remember(existing) { mutableStateOf((existing?.optInt("context_size", 8192) ?: 8192).toString()) }
    var selectedKnowledgeIds by remember(existing, knowledgeLibraries) {
        val initial = mutableSetOf<Int>()
        val array = existing?.optJSONArray("knowledge_library_ids") ?: JSONArray()
        for (index in 0 until array.length()) array.optInt(index).takeIf { it > 0 }?.let(initial::add)
        mutableStateOf(initial.toSet())
    }
    var selectedToolIds by remember(existing, toolOptions) {
        val initial = mutableSetOf<String>()
        val array = existing?.optJSONArray("tools_enabled")
        if (array == null && existing == null) initial += DEFAULT_AGENT_TOOL_IDS
        else if (array != null) for (index in 0 until array.length()) array.optString(index).trim().takeIf { it.isNotBlank() }?.let(initial::add)
        mutableStateOf(initial.toSet())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create Agent" else "Agent Details") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Agent Details", fontWeight = FontWeight.Bold) }
                item { OutlinedTextField(name, { name = it.take(80) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(avatar, { avatar = it.take(8) }, label = { Text("Avatar") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(color, { color = it.take(16) }, label = { Text("Color") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(role, { role = it }, label = { Text("Role summary") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(systemPrompt, { systemPrompt = it }, label = { Text("Character instructions") }, minLines = 4, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(greeting, { greeting = it }, label = { Text("Greeting") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(roleDescription, { roleDescription = it.take(4000) }, label = { Text("Describe an agent to generate") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedButton(
                        onClick = {
                            if (roleDescription.trim().length >= 3) onGenerateRole(roleDescription.trim(), model) { generated ->
                                role = generated.optString("role", role)
                                systemPrompt = generated.optString("system_prompt", systemPrompt)
                                greeting = generated.optString("greeting", greeting)
                            }
                        },
                        enabled = roleDescription.trim().length >= 3,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Generate identity, character, and greeting") }
                }

                item { HorizontalDivider() }
                item { Text("Model / Voice / STT", fontWeight = FontWeight.Bold) }
                item { ChoiceField("LLM model", model, configChoices(configurationOptions, "llm_models"), Modifier.fillMaxWidth()) { model = it } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceField("Language", language, configChoices(configurationOptions, "languages"), Modifier.weight(1f)) { selected ->
                            language = selected
                            val allowed = sttChoices(configurationOptions, selected)
                            if (allowed.isNotEmpty() && allowed.none { it.first == sttModel }) sttModel = allowed.first().first
                            if (selected == "id") ttsMode = "edge"
                            val edge = edgeVoiceChoices(configurationOptions, selected)
                            if (edge.isNotEmpty() && edge.none { it.first == edgeVoice }) edgeVoice = edge.first().first
                        }
                        ChoiceField("TTS mode", ttsMode, configChoices(configurationOptions, "tts_modes"), Modifier.weight(1f)) { ttsMode = it }
                    }
                }
                item { ChoiceField("STT model", sttModel, sttChoices(configurationOptions, language), Modifier.fillMaxWidth()) { sttModel = it } }
                item { ChoiceField("Edge voice", edgeVoice, edgeVoiceChoices(configurationOptions, language), Modifier.fillMaxWidth()) { edgeVoice = it } }
                item { ChoiceField("Kokoro voice", kokoroVoiceId, configChoices(configurationOptions, "kokoro_voices"), Modifier.fillMaxWidth()) { kokoroVoiceId = it } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(ttsRate, { ttsRate = it }, label = { Text("Speech rate") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(ttsVolume, { ttsVolume = it }, label = { Text("Volume") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(temperature, { temperature = it }, label = { Text("Temperature") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                        OutlinedTextField(topP, { topP = it }, label = { Text("Top P") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(maxTokens, { maxTokens = it.filter(Char::isDigit) }, label = { Text("Max tokens") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(contextSize, { contextSize = it.filter(Char::isDigit) }, label = { Text("Context") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                }

                item { HorizontalDivider() }
                item { Text("Tools / Plugins", fontWeight = FontWeight.Bold) }
                if (toolOptions.isEmpty()) {
                    item { Text("No tools reported by Core.", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(toolOptions, key = { "tool-${it.id}" }) { tool ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = tool.id in selectedToolIds,
                                enabled = tool.enabled,
                                onCheckedChange = { checked -> selectedToolIds = if (checked) selectedToolIds + tool.id else selectedToolIds - tool.id },
                            )
                            Column {
                                Text(tool.name, fontWeight = FontWeight.SemiBold)
                                Text(if (tool.enabled) tool.id else "${tool.id} · globally disabled", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                item { HorizontalDivider() }
                item { Text("Knowledge Libraries", fontWeight = FontWeight.Bold) }
                if (knowledgeLibraries.isEmpty()) {
                    item { Text("No Knowledge Libraries yet.", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(knowledgeLibraries, key = { "knowledge-${it.optInt("id")}" }) { library ->
                        val libraryId = library.optInt("id")
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = libraryId in selectedKnowledgeIds, onCheckedChange = { checked -> selectedKnowledgeIds = if (checked) selectedKnowledgeIds + libraryId else selectedKnowledgeIds - libraryId })
                            Column {
                                Text(library.optString("name", "Knowledge"), fontWeight = FontWeight.SemiBold)
                                Text("${library.optInt("document_count")} docs${if (library.optBoolean("enabled", true)) "" else " · disabled"}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) return@TextButton
                val draft = normalizeAgentDraft(
                    AgentDraft(
                        name = name,
                        avatar = avatar,
                        color = color,
                        role = role,
                        systemPrompt = systemPrompt,
                        greeting = greeting,
                        llmModel = model,
                        language = language,
                        ttsMode = ttsMode,
                        edgeVoice = edgeVoice,
                        kokoroVoiceId = kokoroVoiceId.toIntOrNull() ?: 0,
                        ttsRate = ttsRate.toDoubleOrNull() ?: 1.0,
                        ttsVolume = ttsVolume.toDoubleOrNull() ?: 1.0,
                        sttModel = sttModel,
                        temperature = temperature.toDoubleOrNull() ?: 0.6,
                        topP = topP.toDoubleOrNull() ?: 0.9,
                        maxTokens = maxTokens.toIntOrNull() ?: 1024,
                        contextSize = contextSize.toIntOrNull() ?: 8192,
                        toolsEnabled = selectedToolIds,
                        knowledgeLibraryIds = selectedKnowledgeIds,
                    ),
                )
                if (draft.name.isBlank()) return@TextButton
                val payload = existing?.let { JSONObject(it.toString()) } ?: JSONObject()
                payload.put("name", draft.name).put("avatar", draft.avatar).put("color", draft.color)
                    .put("role", draft.role).put("system_prompt", draft.systemPrompt).put("greeting", draft.greeting).put("llm_model", draft.llmModel)
                    .put("language", draft.language).put("tts_mode", draft.ttsMode).put("edge_voice", draft.edgeVoice)
                    .put("kokoro_voice_id", draft.kokoroVoiceId).put("tts_rate", draft.ttsRate).put("tts_volume", draft.ttsVolume)
                    .put("stt_model", draft.sttModel).put("temperature", draft.temperature).put("top_p", draft.topP)
                    .put("max_tokens", draft.maxTokens).put("context_size", draft.contextSize)
                    .put("tools_enabled", JSONArray().apply { draft.toolsEnabled.forEach(::put) })
                    .put("info_ids", JSONArray())
                    .put("knowledge_library_ids", JSONArray().apply { draft.knowledgeLibraryIds.forEach(::put) })
                onSave(existing?.optInt("id")?.takeIf { it > 0 }, payload)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun KnowledgeScreen(viewModel: AppViewModel, activity: MainActivity) {
    val state by viewModel.ui.collectAsState()
    var editingLibrary by remember { mutableStateOf<JSONObject?>(null) }
    var creatingLibrary by remember { mutableStateOf(false) }
    var creatingText by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf<JSONObject?>(null) }
    var retrievalQuery by remember { mutableStateOf("") }
    var catalogQuery by remember { mutableStateOf("") }
    var documentScope by remember { mutableStateOf(KnowledgeDocumentScope.ALL) }
    var statusFilter by remember { mutableStateOf(KnowledgeStatusFilter.ALL) }
    var sourceFilter by remember { mutableStateOf(KnowledgeSourceFilter.ALL) }
    var deleteLibraryTarget by remember { mutableStateOf<JSONObject?>(null) }
    var deleteDocumentTarget by remember { mutableStateOf<JSONObject?>(null) }

    val selectedLibrary = state.knowledgeLibraries.firstOrNull { it.optInt("id") == state.selectedKnowledgeLibraryId }
    val libraryNames = state.knowledgeLibraries.associate { it.optInt("id") to it.optString("name", "Knowledge") }
    val documentRefs = state.knowledgeAllDocuments.map {
        KnowledgeDocumentRef(
            id = it.optInt("id"),
            libraryId = it.optInt("library_id"),
            sourceType = it.optString("source_type"),
            title = it.optString("title"),
            sourceName = it.optString("source_name"),
            status = it.optString("status"),
        )
    }
    val jobRefs = state.knowledgeJobs.mapNotNull { job ->
        val id = job.optInt("id")
        val documentId = job.optInt("document_id")
        if (id <= 0 || documentId <= 0) null else KnowledgeIngestionJobRef(
            id = id,
            documentId = documentId,
            jobType = job.optString("job_type", "ingest"),
            status = job.optString("status", "queued"),
            stage = job.optString("stage", "queued"),
            progress = job.optDouble("progress", 0.0),
            error = job.optString("error").takeIf { it.isNotBlank() },
        )
    }
    val counts = knowledgeOverviewCounts(documentRefs, state.selectedKnowledgeLibraryId)
    val visibleDocuments = state.knowledgeAllDocuments.filter { document ->
        val ref = KnowledgeDocumentRef(
            id = document.optInt("id"),
            libraryId = document.optInt("library_id"),
            sourceType = document.optString("source_type"),
            title = document.optString("title"),
            sourceName = document.optString("source_name"),
            status = document.optString("status"),
        )
        val libraryName = libraryNames[ref.libraryId].orEmpty()
        knowledgeMatchesScope(ref, documentScope, state.selectedKnowledgeLibraryId) &&
            knowledgeMatchesQuery(ref, libraryName, catalogQuery) &&
            knowledgeMatchesStatus(ref, statusFilter) &&
            knowledgeMatchesSourceFilter(ref, sourceFilter)
    }
    val documentHeading = when (documentScope) {
        KnowledgeDocumentScope.ALL -> "All Knowledge"
        KnowledgeDocumentScope.LEGACY -> "Legacy Knowledge"
        KnowledgeDocumentScope.CURRENT -> "Current Knowledge"
        KnowledgeDocumentScope.SELECTED_LIBRARY -> selectedLibrary?.optString("name") ?: "Selected Library"
    }
    val activeJobs = jobRefs.count { it.status.lowercase() in setOf("queued", "running", "processing") }
    val failedJobs = jobRefs.count { it.status.lowercase() in setOf("failed", "error") }
    val filtersActive = catalogQuery.isNotBlank() || statusFilter != KnowledgeStatusFilter.ALL || sourceFilter != KnowledgeSourceFilter.ALL

    fun assignedLibraryIds(agent: JSONObject): Set<Int> = buildSet {
        val array = agent.optJSONArray("knowledge_library_ids") ?: JSONArray()
        for (index in 0 until array.length()) array.optInt(index).takeIf { it > 0 }?.let(::add)
    }

    ManagementSubpage(viewModel, "Knowledge") { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Feedback(viewModel) }
            item {
                val migration = state.knowledgeStatus?.optJSONObject("legacy_information_migration")
                val migratedDocuments = migration?.optInt("migrated_documents", 0) ?: 0
                val migratedLibraries = migration?.optInt("migrated_libraries", 0) ?: 0
                DashboardCard("Knowledge", null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill("${counts.total} SOURCES")
                        Pill("${state.knowledgeLibraries.size} LIBRARIES")
                        if (activeJobs > 0) Pill("$activeJobs PROCESSING")
                        if (failedJobs > 0) Pill("$failedJobs ERRORS")
                    }
                    if (migratedDocuments > 0) {
                        Text(
                            "Legacy migration: $migratedDocuments items across $migratedLibraries libraries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    val indexStatus = migration?.optString("index_status", "pending") ?: "pending"
                    val completed = migration?.optInt("index_completed", 0) ?: 0
                    val total = migration?.optInt("index_total", 0) ?: 0
                    Text(
                        if (indexStatus == "indexing") "Dense indexing in background: $completed/$total" else "Dense index: ${indexStatus.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.knowledgeLoading) Text("Refreshing knowledge…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    state.knowledgeLoadError?.let { error -> Text("Knowledge refresh failed: $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    OutlinedButton(onClick = viewModel::refreshKnowledge, enabled = !state.knowledgeLoading, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(if (state.knowledgeLoading) "Refreshing…" else "Refresh")
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { creatingLibrary = true }, modifier = Modifier.weight(1f)) { Text("＋ Library") }
                    OutlinedButton(onClick = { creatingText = true }, enabled = selectedLibrary != null, modifier = Modifier.weight(1f)) { Text("＋ Text") }
                    OutlinedButton(onClick = activity::chooseKnowledgeForUpload, enabled = selectedLibrary != null, modifier = Modifier.weight(1f)) { Text("Upload file") }
                }
                selectedLibrary?.let { library ->
                    Text(
                        "New knowledge will be added to ${library.optString("name", "the selected library")}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
            item { SectionTitle("Libraries") }
            if (state.knowledgeLibraries.isEmpty() && !state.knowledgeLoading) {
                item {
                    DashboardCard("No Knowledge Libraries yet", "Create a library, then add text or upload a document.") {
                        Text("Legacy knowledge will also appear here automatically after Core migration.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(state.knowledgeLibraries, key = { "library-${it.optInt("id")}" }) { library ->
                val selected = library.optInt("id") == state.selectedKnowledgeLibraryId
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(library.optString("name", "Knowledge"), fontWeight = FontWeight.Bold)
                                Text("${library.optInt("document_count")} documents · ${library.optInt("agent_count")} agents", style = MaterialTheme.typography.bodySmall)
                                library.optString("description").takeIf { it.isNotBlank() }?.let { description ->
                                    Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                }
                            }
                            if (selected) Pill("SELECTED")
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.selectKnowledgeLibrary(library.optInt("id"))
                                    documentScope = KnowledgeDocumentScope.SELECTED_LIBRARY
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Open") }
                            OutlinedButton(onClick = { editingLibrary = library }, modifier = Modifier.weight(1f)) { Text("Edit") }
                            TextButton(onClick = { deleteLibraryTarget = library }) { Text("Delete") }
                        }
                    }
                }
            }
            selectedLibrary?.let { library ->
                item {
                    DashboardCard("Agent access", "Agents allowed to retrieve from ${library.optString("name", "this library")}") {
                        if (state.rawAgents.isEmpty()) {
                            Text("No agents are available.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            state.rawAgents.forEachIndexed { index, agent ->
                                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 5.dp))
                                val libraryId = library.optInt("id")
                                val assigned = libraryId in assignedLibraryIds(agent)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = assigned,
                                        enabled = !state.busy,
                                        onCheckedChange = { checked -> viewModel.setKnowledgeLibraryForAgent(agent.optInt("id"), libraryId, checked) },
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(agent.optString("name", "Agent"), fontWeight = FontWeight.SemiBold)
                                        Text(if (assigned) "Can use this knowledge" else "No access to this library", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Knowledge sources") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected = documentScope == KnowledgeDocumentScope.ALL, onClick = { documentScope = KnowledgeDocumentScope.ALL }, label = { Text("All ${counts.total}") }) }
                    item { FilterChip(selected = documentScope == KnowledgeDocumentScope.LEGACY, onClick = { documentScope = KnowledgeDocumentScope.LEGACY }, label = { Text("Legacy ${counts.legacy}") }) }
                    item { FilterChip(selected = documentScope == KnowledgeDocumentScope.CURRENT, onClick = { documentScope = KnowledgeDocumentScope.CURRENT }, label = { Text("Current ${counts.current}") }) }
                    item { FilterChip(selected = documentScope == KnowledgeDocumentScope.SELECTED_LIBRARY, onClick = { documentScope = KnowledgeDocumentScope.SELECTED_LIBRARY }, enabled = selectedLibrary != null, label = { Text("Selected ${counts.selected}") }) }
                }
                OutlinedTextField(
                    value = catalogQuery,
                    onValueChange = { catalogQuery = it.take(160) },
                    label = { Text("Search sources") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(statusFilter == KnowledgeStatusFilter.ALL, { statusFilter = KnowledgeStatusFilter.ALL }, label = { Text("Any") }) }
                    item { FilterChip(statusFilter == KnowledgeStatusFilter.READY, { statusFilter = KnowledgeStatusFilter.READY }, label = { Text("Ready") }) }
                    item { FilterChip(statusFilter == KnowledgeStatusFilter.PROCESSING, { statusFilter = KnowledgeStatusFilter.PROCESSING }, label = { Text("Processing") }) }
                    item { FilterChip(statusFilter == KnowledgeStatusFilter.ERROR, { statusFilter = KnowledgeStatusFilter.ERROR }, label = { Text("Errors") }) }
                }
                Text("Source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(sourceFilter == KnowledgeSourceFilter.ALL, { sourceFilter = KnowledgeSourceFilter.ALL }, label = { Text("Any") }) }
                    item { FilterChip(sourceFilter == KnowledgeSourceFilter.LEGACY, { sourceFilter = KnowledgeSourceFilter.LEGACY }, label = { Text("Legacy") }) }
                    item { FilterChip(sourceFilter == KnowledgeSourceFilter.TEXT, { sourceFilter = KnowledgeSourceFilter.TEXT }, label = { Text("Text") }) }
                    item { FilterChip(sourceFilter == KnowledgeSourceFilter.FILE, { sourceFilter = KnowledgeSourceFilter.FILE }, label = { Text("Files") }) }
                }
                if (filtersActive) {
                    TextButton(onClick = { catalogQuery = ""; statusFilter = KnowledgeStatusFilter.ALL; sourceFilter = KnowledgeSourceFilter.ALL }) { Text("Clear search filters") }
                }
            }
            item { SectionTitle("$documentHeading · ${visibleDocuments.size} shown") }
            if (visibleDocuments.isEmpty() && !state.knowledgeLoading) {
                item {
                    val emptyText = if (filtersActive) {
                        "No knowledge sources match the current search and filters."
                    } else when (documentScope) {
                        KnowledgeDocumentScope.ALL -> "No knowledge sources yet. Add text or upload a document."
                        KnowledgeDocumentScope.LEGACY -> "No migrated legacy knowledge was found."
                        KnowledgeDocumentScope.CURRENT -> "No current knowledge sources were found."
                        KnowledgeDocumentScope.SELECTED_LIBRARY -> if (selectedLibrary == null) "Select a library first." else "This library has no documents yet."
                    }
                    Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(visibleDocuments, key = { "doc-${it.optInt("id")}" }) { document ->
                val sourceType = document.optString("source_type")
                val sourceLabel = knowledgeSourceLabel(sourceType)
                val libraryId = document.optInt("library_id")
                val libraryName = libraryNames[libraryId] ?: "Library $libraryId"
                val metadata = document.optJSONObject("metadata")
                val chunkCount = metadata?.optInt("chunk_count", 0) ?: 0
                val status = document.optString("status", "registered")
                val editable = sourceType in listOf("manual_text", "legacy_information", "packaged_default")
                val canReprocess = !editable && document.optString("storage_key").isNotBlank()
                val latestJob = latestKnowledgeJob(document.optInt("id"), jobRefs)
                val jobStatus = latestJob?.status?.lowercase().orEmpty()
                val jobActive = jobStatus in setOf("queued", "running", "processing")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(document.optString("title", "Document"), fontWeight = FontWeight.Bold)
                                Text("$sourceLabel · $chunkCount chunks · $libraryName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Pill(status.uppercase())
                        }
                        latestJob?.let { job ->
                            val kind = if (job.jobType.equals("reingest", true)) "Reprocess" else "Ingest"
                            Text(
                                "$kind · ${job.status.uppercase()} · ${job.stage} · ${knowledgeJobProgressPercent(job)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (job.status.equals("failed", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            job.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                        }
                        document.optString("error").takeIf { it.isNotBlank() }?.let { error ->
                            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { viewModel.loadKnowledgeDocument(document.optInt("id")) }, modifier = Modifier.weight(1f)) { Text("Inspect") }
                            if (editable) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.selectKnowledgeLibrary(libraryId)
                                        viewModel.loadKnowledgeDocument(document.optInt("id"))
                                        editingText = document
                                        documentScope = KnowledgeDocumentScope.SELECTED_LIBRARY
                                    },
                                    modifier = Modifier.weight(1f),
                                ) { Text("Edit") }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.reindexKnowledgeDocument(document.optInt("id")) },
                                enabled = !jobActive,
                                modifier = Modifier.weight(1f),
                            ) { Text("Reindex") }
                            OutlinedButton(
                                onClick = { viewModel.reingestKnowledgeDocument(document.optInt("id")) },
                                enabled = canReprocess && !jobActive,
                                modifier = Modifier.weight(1f),
                            ) { Text(if (jobActive) "Processing…" else "Reprocess") }
                        }
                        Text(
                            if (canReprocess) "Reindex refreshes retrieval only. Reprocess reparses the original file and rebuilds its chunks." else "Text sources can be edited and reindexed; file reprocessing requires an original stored file.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                        TextButton(onClick = { deleteDocumentTarget = document }) { Text("Delete") }
                    }
                }
            }
            item {
                DashboardCard("Retrieval test", "Inspect what the selected library returns before Chat uses it") {
                    OutlinedTextField(retrievalQuery, { retrievalQuery = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { if (retrievalQuery.isNotBlank()) viewModel.searchKnowledge(retrievalQuery) }, enabled = selectedLibrary != null, modifier = Modifier.weight(1f)) { Text("Search") }
                        OutlinedButton(onClick = viewModel::rebuildKnowledgeIndex, enabled = selectedLibrary != null, modifier = Modifier.weight(1f)) { Text("Rebuild") }
                    }
                    state.knowledgeSearchResult?.let { result ->
                        val confidence = result.optJSONObject("confidence")
                        Text("Confidence: ${confidence?.optString("label", "none")} · ${"%.3f".format(confidence?.optDouble("score", 0.0) ?: 0.0)}", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
                        val results = result.optJSONArray("results") ?: JSONArray()
                        for (index in 0 until minOf(4, results.length())) {
                            val hit = results.optJSONObject(index) ?: continue
                            Text("K${index + 1} · ${hit.optString("document_title", hit.optString("source_name", "Knowledge"))}${hit.optString("heading_path").takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    if (creatingLibrary || editingLibrary != null) {
        KnowledgeLibraryDialog(editingLibrary, { creatingLibrary = false; editingLibrary = null }) { id, name, description, enabled ->
            creatingLibrary = false; editingLibrary = null; viewModel.saveKnowledgeLibrary(id, name, description, enabled)
        }
    }
    if (creatingText || editingText != null) {
        KnowledgeTextDialog(editingText, state.knowledgeDocumentContent, { creatingText = false; editingText = null; viewModel.clearKnowledgeDocument() }) { id, title, text ->
            creatingText = false; editingText = null; viewModel.saveKnowledgeText(id, title, text)
        }
    }
    state.knowledgeDocumentContent?.takeIf { editingText == null }?.let { content ->
        val document = content.optJSONObject("document")
        val libraryName = document?.optInt("library_id")?.let { libraryNames[it] }
        KnowledgeInspectDialog(content, libraryName, onDismiss = viewModel::clearKnowledgeDocument)
    }
    deleteLibraryTarget?.let { library ->
        ConfirmDialog(
            title = "Delete ${library.optString("name", "Knowledge Library")}?",
            message = "This permanently removes ${library.optInt("document_count")} knowledge sources and disconnects this library from ${library.optInt("agent_count")} agents. This cannot be undone.",
            onDismiss = { deleteLibraryTarget = null },
            onConfirm = { val id = library.optInt("id"); deleteLibraryTarget = null; viewModel.deleteKnowledgeLibrary(id) },
        )
    }
    deleteDocumentTarget?.let { document ->
        val libraryName = libraryNames[document.optInt("library_id")] ?: "its library"
        ConfirmDialog(
            title = "Delete ${document.optString("title", "knowledge source")}?",
            message = "This permanently removes the source, parsed chunks, and retrieval index data from $libraryName. This cannot be undone.",
            onDismiss = { deleteDocumentTarget = null },
            onConfirm = { val id = document.optInt("id"); deleteDocumentTarget = null; viewModel.deleteKnowledgeDocument(id) },
        )
    }
}

@Composable
private fun KnowledgeLibraryDialog(existing: JSONObject?, onDismiss: () -> Unit, onSave: (Int?, String, String, Boolean) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.optString("name") ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.optString("description") ?: "") }
    var enabled by remember(existing) { mutableStateOf(existing?.optBoolean("enabled", true) ?: true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Create library" else "Edit library") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it.take(120) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it.take(4000) }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(enabled, { enabled = it }); Text("Enabled for retrieval") }
        }
    }, confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(existing?.optInt("id")?.takeIf { it > 0 }, name, description, enabled) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun KnowledgeTextDialog(existing: JSONObject?, content: JSONObject?, onDismiss: () -> Unit, onSave: (Int?, String, String) -> Unit) {
    var title by remember(existing) { mutableStateOf(existing?.optString("title") ?: "") }
    val existingText = remember(content) {
        val blocks = content?.optJSONArray("parent_blocks") ?: JSONArray()
        buildString { for (index in 0 until blocks.length()) { val text = blocks.optJSONObject(index)?.optString("text").orEmpty(); if (text.isNotBlank()) { if (isNotEmpty()) append("\n\n"); append(text) } } }
    }
    var text by remember(existing, existingText) { mutableStateOf(existingText) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Add knowledge text" else "Edit knowledge text") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it.take(240) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(text, { text = it }, label = { Text("Knowledge") }, minLines = 8, modifier = Modifier.fillMaxWidth())
            Text("This text is retrieved only when relevant.", style = MaterialTheme.typography.labelSmall)
        }
    }, confirmButton = { TextButton(onClick = { if (title.isNotBlank() && text.isNotBlank()) onSave(existing?.optInt("id")?.takeIf { it > 0 }, title, text) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun KnowledgeInspectDialog(content: JSONObject, libraryName: String?, onDismiss: () -> Unit) {
    val document = content.optJSONObject("document") ?: JSONObject()
    val chunks = content.optJSONArray("chunks") ?: JSONArray()
    val sourceLabel = knowledgeSourceLabel(document.optString("source_type"))
    val chunkTotal = content.optInt("chunks_total", chunks.length())
    val blockTotal = content.optInt("parent_blocks_total", content.optJSONArray("parent_blocks")?.length() ?: 0)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(document.optString("title", "Knowledge document")) }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${sourceLabel} · ${document.optString("status", "registered")}", fontWeight = FontWeight.SemiBold)
                        libraryName?.let { Text("Library: $it", style = MaterialTheme.typography.bodySmall) }
                        document.optString("source_name").takeIf { it.isNotBlank() }?.let { Text("Source: $it", style = MaterialTheme.typography.bodySmall) }
                        Text("$chunkTotal chunks · $blockTotal parent blocks", style = MaterialTheme.typography.bodySmall)
                        document.optString("error").takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            items(minOf(chunks.length(), 12)) { index ->
                val chunk = chunks.optJSONObject(index) ?: JSONObject()
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Chunk ${index + 1}", fontWeight = FontWeight.Bold)
                        chunk.optString("heading_path").takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        Text(chunk.optString("text"), style = MaterialTheme.typography.bodySmall, maxLines = 8)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
internal fun ScriptsScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    var creating by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("queue") }
    var query by remember { mutableStateOf("") }
    val visibleScripts = state.scriptItems.filter { script ->
        val needle = query.trim().lowercase()
        needle.isBlank() || script.optString("title").lowercase().contains(needle) || script.optString("text").lowercase().contains(needle)
    }
    ManagementScaffold(viewModel, "Scripts", AppScreen.SCRIPTS) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Feedback(viewModel) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tab == "queue") Button(onClick = { tab = "queue" }, modifier = Modifier.weight(1f)) { Text("Queue") }
                    else OutlinedButton(onClick = { tab = "queue" }, modifier = Modifier.weight(1f)) { Text("Queue") }
                    if (tab == "scripts") Button(onClick = { tab = "scripts" }, modifier = Modifier.weight(1f)) { Text("Scripts") }
                    else OutlinedButton(onClick = { tab = "scripts" }, modifier = Modifier.weight(1f)) { Text("Scripts") }
                }
            }
            if (tab == "queue") {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Playback Queue (${state.queueItems.size})", fontWeight = FontWeight.Bold)
                                    Text("State: ${state.queueState}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Pill(if (state.queueLoop) "LOOP" else "ONCE")
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                QueueTransportControl(
                                    icon = Icons.Outlined.PlayArrow,
                                    label = "Play",
                                    active = state.queueState == "playing",
                                    onClick = { viewModel.queueAction("play") },
                                    modifier = Modifier.weight(1f),
                                )
                                QueueTransportControl(
                                    icon = Icons.Outlined.Pause,
                                    label = "Pause",
                                    active = state.queueState == "paused",
                                    onClick = { viewModel.queueAction("pause") },
                                    modifier = Modifier.weight(1f),
                                )
                                QueueTransportControl(
                                    icon = Icons.Outlined.Stop,
                                    label = "Stop",
                                    onClick = { viewModel.queueAction("stop") },
                                    modifier = Modifier.weight(1f),
                                )
                                QueueTransportControl(
                                    icon = Icons.Outlined.Repeat,
                                    label = "Loop",
                                    active = state.queueLoop,
                                    onClick = { viewModel.setQueueLoop(!state.queueLoop) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (state.queueItems.isEmpty()) {
                                Text("Queue is empty. Add a saved script from the Scripts tab.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                            } else {
                                state.queueItems.forEachIndexed { index, item ->
                                    HorizontalDivider(Modifier.padding(vertical = 7.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${index + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(item.optString("title", item.optString("script_title", "Queued script")), fontWeight = FontWeight.SemiBold)
                                            Text("Pause ${item.optDouble("pause_after_seconds", 0.0)} sec", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        TextButton(onClick = { viewModel.removeQueueItem(item.optInt("id")) }) { Text("Remove") }
                                    }
                                }
                            }
                            OutlinedButton(onClick = { viewModel.queueAction("clear") }, enabled = state.queueItems.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Clear queue") }
                        }
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(120) },
                        label = { Text("Search scripts") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (visibleScripts.isEmpty()) {
                    item { Text("No saved scripts match this search.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(visibleScripts, key = { it.optInt("id") }) { script ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(script.optString("title", "Script"), fontWeight = FontWeight.Bold)
                                    Text(script.optString("text"), style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!script.optBoolean("enabled", true)) Pill("DISABLED")
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { viewModel.runScriptNow(script.optInt("id")) }, enabled = script.optBoolean("enabled", true), modifier = Modifier.weight(1f)) { Text("Run") }
                                OutlinedButton(onClick = { viewModel.queueScript(script.optInt("id")) }, enabled = script.optBoolean("enabled", true), modifier = Modifier.weight(1f)) { Text("Queue") }
                                OutlinedButton(onClick = { editing = script }, modifier = Modifier.weight(1f)) { Text("Edit") }
                            }
                            TextButton(onClick = { viewModel.deleteScript(script.optInt("id")) }) { Text("Delete") }
                        }
                    }
                }
                item { Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Create Script") } }
            }
        }
    }
    if (creating || editing != null) {
        ScriptDialog(editing, state.scriptDefaults, state.configurationOptions, { creating = false; editing = null }) { id, payload ->
            creating = false; editing = null; viewModel.saveScript(id, payload)
        }
    }
}

@Composable
private fun QueueTransportControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun QueueItemRow(viewModel: AppViewModel, item: JSONObject) {
    val id = item.optInt("id")
    var dragOffset by remember(id) { mutableStateOf(0f) }
    var pauseText by remember(id, item.optDouble("pause_after_seconds", 0.0)) {
        mutableStateOf(item.optDouble("pause_after_seconds", 0.0).toString().removeSuffix(".0"))
    }
    val threshold = with(LocalDensity.current) { 46.dp.toPx() }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "☰",
                modifier = Modifier.padding(end = 10.dp).pointerInput(id) {
                    detectDragGesturesAfterLongPress(
                        onDragEnd = { dragOffset = 0f },
                        onDragCancel = { dragOffset = 0f },
                    ) { change, amount ->
                        change.consume()
                        dragOffset += amount.y
                        if (dragOffset >= threshold) { dragOffset = 0f; viewModel.moveQueueItem(id, 1) }
                        else if (dragOffset <= -threshold) { dragOffset = 0f; viewModel.moveQueueItem(id, -1) }
                    }
                },
                fontWeight = FontWeight.Bold,
            )
            Column(Modifier.weight(1f)) {
                Text(item.optString("title", item.optString("script_title", "Queued script")), fontWeight = FontWeight.SemiBold)
                Text("Long-press and drag ☰ to reorder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { viewModel.removeQueueItem(id) }) { Text("Remove") }
        }
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(
                pauseText,
                { pauseText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(7) },
                label = { Text("Pause after (sec)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedButton(onClick = { viewModel.setQueuePause(id, pauseText.toDoubleOrNull() ?: 0.0) }) { Text("Set") }
        }
    }
}

@Composable
private fun ScriptDialog(
    existing: JSONObject?,
    rememberedDefaults: JSONObject?,
    configurationOptions: JSONObject?,
    onDismiss: () -> Unit,
    onSave: (Int?, JSONObject) -> Unit,
) {
    val source = existing ?: rememberedDefaults ?: JSONObject()
    val stateKey = "${existing?.optInt("id", 0) ?: 0}:${source.toString()}"
    var title by remember(stateKey) { mutableStateOf(existing?.optString("title") ?: "") }
    var text by remember(stateKey) { mutableStateOf(existing?.optString("text") ?: "") }
    var enabled by remember(stateKey) { mutableStateOf(existing?.optBoolean("enabled", true) ?: true) }
    var language by remember(stateKey) { mutableStateOf(source.optString("language", "en")) }
    var ttsMode by remember(stateKey) { mutableStateOf(source.optString("tts_mode", "edge")) }
    var edgeVoice by remember(stateKey) { mutableStateOf(source.optString("edge_voice", "en-US-AriaNeural")) }
    var kokoroId by remember(stateKey) { mutableStateOf(source.optInt("kokoro_voice_id", 0).toString()) }
    var rate by remember(stateKey) { mutableStateOf(source.optDouble("tts_rate", 1.0).toString()) }
    var volume by remember(stateKey) { mutableStateOf(source.optDouble("tts_volume", 1.0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create script" else "Edit script") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(title, { title = it.take(120) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(text, { text = it }, label = { Text("Script text") }, minLines = 5, modifier = Modifier.fillMaxWidth()) }
                item {
                    Text(
                        if (existing == null) "Speech settings start from the last script you saved. If none exists yet, VerbaNode uses the normal defaults."
                        else "This script keeps its own saved speech settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceField("Language", language, configChoices(configurationOptions, "languages"), Modifier.weight(1f)) { selected ->
                            language = selected
                            if (language == "id") {
                                ttsMode = "edge"
                                if (!edgeVoice.startsWith("id-")) edgeVoice = "id-ID-GadisNeural"
                            } else if (edgeVoice.startsWith("id-")) {
                                edgeVoice = "en-US-AriaNeural"
                            }
                        }
                        ChoiceField("TTS mode", ttsMode, configChoices(configurationOptions, "tts_modes"), Modifier.weight(1f)) { selected ->
                            if (language != "id" || selected == "edge") ttsMode = selected
                        }
                    }
                }
                item {
                    ChoiceField(
                        "Edge voice",
                        edgeVoice,
                        edgeVoiceChoices(configurationOptions, language),
                        Modifier.fillMaxWidth(),
                    ) { edgeVoice = it }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            kokoroId,
                            { kokoroId = it.filter(Char::isDigit) },
                            label = { Text("Kokoro ID") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = language != "id",
                        )
                        OutlinedTextField(
                            rate,
                            { rate = it },
                            label = { Text("Rate") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            volume,
                            { volume = it },
                            label = { Text("Volume") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(enabled, { enabled = it }); Text("Enabled") } }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank() || text.isBlank()) return@TextButton
                val normalizedLanguage = if (language == "id") "id" else "en"
                val normalizedMode = if (normalizedLanguage == "id") "edge" else ttsMode.ifBlank { "edge" }
                val defaultVoice = if (normalizedLanguage == "id") "id-ID-GadisNeural" else "en-US-AriaNeural"
                val normalizedVoice = edgeVoice.ifBlank { defaultVoice }
                val payload = JSONObject()
                    .put("title", title.trim())
                    .put("text", text.trim())
                    .put("enabled", enabled)
                    .put("language", normalizedLanguage)
                    .put("tts_mode", normalizedMode)
                    .put("edge_voice", normalizedVoice)
                    .put("kokoro_voice_id", kokoroId.toIntOrNull() ?: 0)
                    .put("tts_rate", (rate.toDoubleOrNull() ?: 1.0).coerceIn(0.5, 2.0))
                    .put("tts_volume", (volume.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 1.0))
                onSave(existing?.optInt("id")?.takeIf { it > 0 }, payload)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun PluginsScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    ManagementSubpage(viewModel, "Plugins") { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Feedback(viewModel) }
            item {
                DashboardCard("Plugin manager", "${state.pluginItems.size} plugins · ${state.pluginSummary?.optInt("enabled", 0) ?: 0} enabled") {
                    Text(
                        "Refresh rereads the current plugin state. Reload asks Core to reload external plugin files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::refreshPlugins, modifier = Modifier.weight(1f)) { Text("Refresh") }
                        OutlinedButton(onClick = viewModel::reloadPlugins, modifier = Modifier.weight(1f)) { Text("Reload external") }
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetPluginMetrics(null) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) { Text("Reset all metrics") }
                }
            }
            items(state.pluginItems, key = { it.optString("id") }) { plugin ->
                val id = plugin.optString("id")
                val enabled = plugin.optBoolean("enabled", true)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(plugin.optString("name", id), fontWeight = FontWeight.Bold)
                                Text(id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = enabled, onCheckedChange = { viewModel.setPluginEnabled(id, it) })
                        }
                        Text(plugin.optString("description", plugin.optString("detail", "")), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(plugin.optString("status", if (enabled) "enabled" else "disabled").uppercase())
                            val calls = plugin.optInt("call_count", plugin.optInt("calls", 0))
                            if (calls > 0) Pill("$calls CALLS")
                            val agents = plugin.optInt("agent_count", 0)
                            if (agents > 0) Pill("$agents AGENTS")
                        }
                        plugin.optString("last_error").takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { viewModel.reloadPlugin(id) }, modifier = Modifier.weight(1f)) { Text("Reload") }
                            OutlinedButton(onClick = { viewModel.recoverPlugin(id) }, modifier = Modifier.weight(1f)) { Text("Recover") }
                            OutlinedButton(onClick = { viewModel.resetPluginMetrics(id) }, modifier = Modifier.weight(1f)) { Text("Metrics") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ConfirmDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ManagementSubpage(viewModel: AppViewModel, title: String, content: @Composable (PaddingValues) -> Unit) {
    androidx.compose.material3.Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = { viewModel.navigate(AppScreen.MORE) }) { Text("‹ Back") } },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        bottomBar = { MainBottomNavigation(viewModel, AppScreen.MORE) },
        content = content,
    )
}
