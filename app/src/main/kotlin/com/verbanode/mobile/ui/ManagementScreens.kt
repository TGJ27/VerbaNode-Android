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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.verbanode.mobile.AppScreen
import com.verbanode.mobile.AppViewModel
import com.verbanode.mobile.MainActivity
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
    ManagementScaffold(viewModel, "Agents", AppScreen.AGENTS) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Feedback(viewModel) }
            item {
                DashboardCard("Agent workspace", "Create and manage the same agents used by the web dashboard.") {
                    Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Create agent") }
                }
            }
            items(state.rawAgents, key = { it.optInt("id") }) { agent ->
                val id = agent.optInt("id")
                val active = state.activeAgent?.id == id
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(agent.optString("name", "Agent"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (active) Pill("ACTIVE")
                                }
                                Text(agent.optString("role"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!active) Button(onClick = { viewModel.selectAgent(id) }) { Text("Activate") }
                        }
                        Row(Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(agent.optString("language", "en").uppercase())
                            Pill(agent.optString("llm_model", "model"))
                            Pill(agent.optString("tts_mode", "tts"))
                        }
                        Text(agent.optString("greeting"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { editing = agent }, modifier = Modifier.weight(1f)) { Text("Edit") }
                            OutlinedButton(onClick = { viewModel.clearAgentMemoryManagement(id) }, modifier = Modifier.weight(1f)) { Text("Clear memory") }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.exportAgent(id) { bytes, name, mime -> activity.saveDocument(bytes, name, mime) } },
                                modifier = Modifier.weight(1f),
                            ) { Text("Backup") }
                            OutlinedButton(onClick = { deleteTarget = agent }, modifier = Modifier.weight(1f)) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
    if (creating || editing != null) {
        AgentEditorDialog(existing = editing, configurationOptions = state.configurationOptions, knowledgeLibraries = state.knowledgeLibraries, onDismiss = { creating = false; editing = null }) { id, payload ->
            creating = false; editing = null; viewModel.saveAgent(id, payload)
        }
    }
    deleteTarget?.let { agent ->
        ConfirmDialog(
            title = "Delete ${agent.optString("name", "agent")}?",
            message = "This removes the agent configuration. This cannot be undone.",
            onDismiss = { deleteTarget = null },
            onConfirm = { val id = agent.optInt("id"); deleteTarget = null; viewModel.deleteAgentManagement(id) },
        )
    }
}

@Composable
private fun AgentEditorDialog(existing: JSONObject?, configurationOptions: JSONObject?, knowledgeLibraries: List<JSONObject>, onDismiss: () -> Unit, onSave: (Int?, JSONObject) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.optString("name", "Ropi") ?: "Ropi") }
    var avatar by remember(existing) { mutableStateOf(existing?.optString("avatar", "RP") ?: "RP") }
    var color by remember(existing) { mutableStateOf(existing?.optString("color", "#3578f6") ?: "#3578f6") }
    var role by remember(existing) { mutableStateOf(existing?.optString("role", "Helpful assistant") ?: "Helpful assistant") }
    var systemPrompt by remember(existing) { mutableStateOf(existing?.optString("system_prompt", "You are a helpful assistant.") ?: "You are a helpful assistant.") }
    var greeting by remember(existing) { mutableStateOf(existing?.optString("greeting", "Hello. How can I help?") ?: "Hello. How can I help?") }
    var model by remember(existing) { mutableStateOf(existing?.optString("llm_model", "qwen3.5:0.8b") ?: "qwen3.5:0.8b") }
    var language by remember(existing) { mutableStateOf(existing?.optString("language", "en") ?: "en") }
    var ttsMode by remember(existing) { mutableStateOf(existing?.optString("tts_mode", "edge_fallback") ?: "edge_fallback") }
    var edgeVoice by remember(existing) { mutableStateOf(existing?.optString("edge_voice", "en-US-AriaNeural") ?: "en-US-AriaNeural") }
    var sttModel by remember(existing) { mutableStateOf(existing?.optString("stt_model", "iic/SenseVoiceSmall") ?: "iic/SenseVoiceSmall") }
    var temperature by remember(existing) { mutableStateOf(existing?.optDouble("temperature", 0.6)?.toString() ?: "0.6") }
    var topP by remember(existing) { mutableStateOf(existing?.optDouble("top_p", 0.9)?.toString() ?: "0.9") }
    var maxTokens by remember(existing) { mutableStateOf(existing?.optInt("max_tokens", 1024)?.toString() ?: "1024") }
    var contextSize by remember(existing) { mutableStateOf(existing?.optInt("context_size", 8192)?.toString() ?: "8192") }
    var selectedKnowledgeIds by remember(existing, knowledgeLibraries) {
        val initial = mutableSetOf<Int>()
        val array = existing?.optJSONArray("knowledge_library_ids") ?: JSONArray()
        for (index in 0 until array.length()) array.optInt(index).takeIf { it > 0 }?.let(initial::add)
        mutableStateOf(initial.toSet())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create agent" else "Edit agent") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it.take(80) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(avatar, { avatar = it.take(8) }, label = { Text("Avatar") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(color, { color = it.take(16) }, label = { Text("Color") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(role, { role = it }, label = { Text("Role") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(systemPrompt, { systemPrompt = it }, label = { Text("System prompt") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(greeting, { greeting = it }, label = { Text("Greeting") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
                item { ChoiceField("LLM model", model, configChoices(configurationOptions, "llm_models"), Modifier.fillMaxWidth()) { model = it } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceField("Language", language, configChoices(configurationOptions, "languages"), Modifier.weight(1f)) { selected ->
                            language = selected
                            val allowed = sttChoices(configurationOptions, selected)
                            if (allowed.isNotEmpty() && allowed.none { it.first == sttModel }) sttModel = allowed.first().first
                        }
                        ChoiceField("TTS mode", ttsMode, configChoices(configurationOptions, "tts_modes"), Modifier.weight(1f)) { ttsMode = it }
                    }
                }
                item { OutlinedTextField(edgeVoice, { edgeVoice = it }, label = { Text("Edge voice") }, modifier = Modifier.fillMaxWidth()) }
                item { ChoiceField("STT model", sttModel, sttChoices(configurationOptions, language), Modifier.fillMaxWidth()) { sttModel = it } }
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
                item { Text("Knowledge Libraries", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) }
                if (knowledgeLibraries.isEmpty()) {
                    item { Text("No Knowledge Libraries yet.", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(knowledgeLibraries, key = { "knowledge-${it.optInt("id")}" }) { library ->
                        val libraryId = library.optInt("id")
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = libraryId in selectedKnowledgeIds, onCheckedChange = { checked -> selectedKnowledgeIds = if (checked) selectedKnowledgeIds + libraryId else selectedKnowledgeIds - libraryId })
                            Column { Text(library.optString("name", "Knowledge"), fontWeight = FontWeight.SemiBold); Text("${library.optInt("document_count")} docs", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) return@TextButton
                val payload = existing?.let { JSONObject(it.toString()) } ?: JSONObject()
                payload.put("name", name.trim()).put("avatar", avatar.ifBlank { "AI" }).put("color", color.ifBlank { "#3578f6" })
                    .put("role", role).put("system_prompt", systemPrompt).put("greeting", greeting).put("llm_model", model)
                    .put("language", if (language == "id") "id" else "en").put("tts_mode", ttsMode.ifBlank { "edge_fallback" })
                    .put("edge_voice", edgeVoice).put("stt_model", sttModel)
                    .put("temperature", temperature.toDoubleOrNull() ?: 0.6).put("top_p", topP.toDoubleOrNull() ?: 0.9)
                    .put("max_tokens", maxTokens.toIntOrNull() ?: 1024).put("context_size", contextSize.toIntOrNull() ?: 8192)
                    .put("knowledge_library_ids", JSONArray().apply { selectedKnowledgeIds.sorted().forEach { put(it) } })
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
    var query by remember { mutableStateOf("") }
    val selectedLibrary = state.knowledgeLibraries.firstOrNull { it.optInt("id") == state.selectedKnowledgeLibraryId }
    ManagementSubpage(viewModel, "Knowledge") { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Feedback(viewModel) }
            item {
                val migration = state.knowledgeStatus?.optJSONObject("legacy_information_migration")
                DashboardCard("Hybrid RAG", "Only relevant evidence is sent to the LLM") {
                    Text("${state.knowledgeLibraries.size} libraries · ${state.knowledgeDocuments.size} documents in selected library", style = MaterialTheme.typography.bodySmall)
                    val indexStatus = migration?.optString("index_status", "pending") ?: "pending"
                    val completed = migration?.optInt("index_completed", 0) ?: 0
                    val total = migration?.optInt("index_total", 0) ?: 0
                    Text(if (indexStatus == "indexing") "Dense indexing in background: $completed/$total" else "Dense index: ${indexStatus.uppercase()}", style = MaterialTheme.typography.bodySmall)
                    Text("BM25 remains available while dense indexing runs.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { creatingLibrary = true }, modifier = Modifier.weight(1f)) { Text("＋ Library") }
                    OutlinedButton(onClick = { creatingText = true }, enabled = selectedLibrary != null, modifier = Modifier.weight(1f)) { Text("＋ Text") }
                    OutlinedButton(onClick = activity::chooseKnowledgeForUpload, enabled = selectedLibrary != null, modifier = Modifier.weight(1f)) { Text("Upload") }
                }
            }
            item { SectionTitle("Libraries") }
            items(state.knowledgeLibraries, key = { "library-${it.optInt("id")}" }) { library ->
                val selected = library.optInt("id") == state.selectedKnowledgeLibraryId
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(library.optString("name", "Knowledge"), fontWeight = FontWeight.Bold)
                                Text("${library.optInt("document_count")} documents · ${library.optInt("agent_count")} agents", style = MaterialTheme.typography.bodySmall)
                            }
                            if (selected) Pill("SELECTED")
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { viewModel.selectKnowledgeLibrary(library.optInt("id")) }, modifier = Modifier.weight(1f)) { Text("Open") }
                            OutlinedButton(onClick = { editingLibrary = library }, modifier = Modifier.weight(1f)) { Text("Edit") }
                            TextButton(onClick = { viewModel.deleteKnowledgeLibrary(library.optInt("id")) }) { Text("Delete") }
                        }
                    }
                }
            }
            item { SectionTitle(selectedLibrary?.optString("name") ?: "Documents") }
            if (selectedLibrary == null) item { Text("Create or select a Knowledge Library first.") }
            items(state.knowledgeDocuments, key = { "doc-${it.optInt("id")}" }) { document ->
                val sourceType = document.optString("source_type")
                val editable = sourceType in listOf("manual_text", "legacy_information", "packaged_default")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(document.optString("title", "Document"), fontWeight = FontWeight.Bold)
                                Text("$sourceType · ${document.optString("status", "registered")}", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { viewModel.loadKnowledgeDocument(document.optInt("id")) }) { Text("Inspect") }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (editable) OutlinedButton(onClick = { viewModel.loadKnowledgeDocument(document.optInt("id")); editingText = document }, modifier = Modifier.weight(1f)) { Text("Edit") }
                            OutlinedButton(onClick = { viewModel.reindexKnowledgeDocument(document.optInt("id")) }, modifier = Modifier.weight(1f)) { Text("Reindex") }
                            TextButton(onClick = { viewModel.deleteKnowledgeDocument(document.optInt("id")) }) { Text("Delete") }
                        }
                    }
                }
            }
            item {
                DashboardCard("Retrieval test", "Inspect what this library returns before Chat uses it") {
                    OutlinedTextField(query, { query = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { if (query.isNotBlank()) viewModel.searchKnowledge(query) }, modifier = Modifier.weight(1f)) { Text("Search") }
                        OutlinedButton(onClick = viewModel::rebuildKnowledgeIndex, modifier = Modifier.weight(1f)) { Text("Rebuild") }
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
        KnowledgeInspectDialog(content, onDismiss = viewModel::clearKnowledgeDocument)
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
private fun KnowledgeInspectDialog(content: JSONObject, onDismiss: () -> Unit) {
    val document = content.optJSONObject("document") ?: JSONObject()
    val chunks = content.optJSONArray("chunks") ?: JSONArray()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(document.optString("title", "Knowledge document")) }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("${document.optString("source_type")} · ${chunks.length()} chunks", style = MaterialTheme.typography.bodySmall) }
            items(minOf(chunks.length(), 12)) { index ->
                val chunk = chunks.optJSONObject(index) ?: JSONObject()
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp)) { Text("Chunk ${index + 1}", fontWeight = FontWeight.Bold); Text(chunk.optString("heading_path"), style = MaterialTheme.typography.labelSmall); Text(chunk.optString("text"), style = MaterialTheme.typography.bodySmall, maxLines = 8) } }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
internal fun ScriptsScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    var creating by remember { mutableStateOf(false) }
    ManagementScaffold(viewModel, "Scripts & Queue", AppScreen.SCRIPTS) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Feedback(viewModel) }
            item { Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Create script") } }
            item {
                DashboardCard("Queue", "State: ${state.queueState}") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { viewModel.queueAction("play") }, modifier = Modifier.weight(1f)) { Text("Play") }
                        OutlinedButton(onClick = { viewModel.queueAction("pause") }, modifier = Modifier.weight(1f)) { Text("Pause") }
                        OutlinedButton(onClick = { viewModel.queueAction("stop") }, modifier = Modifier.weight(1f)) { Text("Stop") }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Loop queue", fontWeight = FontWeight.SemiBold)
                            Text("Repeat from the top until stopped.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.queueLoop, onCheckedChange = viewModel::setQueueLoop)
                    }
                    OutlinedButton(onClick = { viewModel.queueAction("clear") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Clear queue") }
                    state.queueItems.forEach { item ->
                        HorizontalDivider(Modifier.padding(vertical = 7.dp))
                        QueueItemRow(viewModel, item)
                    }
                }
            }
            item { SectionTitle("Scripts") }
            items(state.scriptItems, key = { it.optInt("id") }) { script ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(script.optString("title", "Script"), fontWeight = FontWeight.Bold)
                                Text("${script.optString("language", "en")} · ${script.optString("tts_mode", "edge")}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (!script.optBoolean("enabled", true)) Pill("DISABLED")
                        }
                        Text(script.optString("text"), style = MaterialTheme.typography.bodySmall, maxLines = 4, modifier = Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { viewModel.runScriptNow(script.optInt("id")) }, enabled = script.optBoolean("enabled", true), modifier = Modifier.weight(1f)) { Text("Run") }
                            OutlinedButton(onClick = { viewModel.queueScript(script.optInt("id")) }, enabled = script.optBoolean("enabled", true), modifier = Modifier.weight(1f)) { Text("Queue") }
                            OutlinedButton(onClick = { editing = script }, modifier = Modifier.weight(1f)) { Text("Edit") }
                        }
                        TextButton(onClick = { viewModel.deleteScript(script.optInt("id")) }) { Text("Delete") }
                    }
                }
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
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = { viewModel.navigate(AppScreen.MORE) }) { Text("‹ Back") } },
            )
        },
        content = content,
    )
}
