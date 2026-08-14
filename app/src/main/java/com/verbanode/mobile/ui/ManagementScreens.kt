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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.verbanode.mobile.AppScreen
import com.verbanode.mobile.AppViewModel
import com.verbanode.mobile.MainActivity
import org.json.JSONObject

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
        AgentEditorDialog(existing = editing, onDismiss = { creating = false; editing = null }) { id, payload ->
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
private fun AgentEditorDialog(existing: JSONObject?, onDismiss: () -> Unit, onSave: (Int?, JSONObject) -> Unit) {
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
                item { OutlinedTextField(model, { model = it }, label = { Text("LLM model") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(language, { language = it.take(2).lowercase() }, label = { Text("Language en/id") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(ttsMode, { ttsMode = it }, label = { Text("TTS mode") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(edgeVoice, { edgeVoice = it }, label = { Text("Edge voice") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(sttModel, { sttModel = it }, label = { Text("STT model") }, modifier = Modifier.fillMaxWidth()) }
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
                onSave(existing?.optInt("id")?.takeIf { it > 0 }, payload)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun InformationScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    var creating by remember { mutableStateOf(false) }
    ManagementSubpage(viewModel, "Information") { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Feedback(viewModel) }
            item { Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Text("＋ Add information") } }
            items(state.informationItems, key = { it.optInt("id") }) { item ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.optString("title", "Information"), fontWeight = FontWeight.Bold)
                                Text(if (item.optBoolean("enabled", true)) "Enabled" else "Disabled", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { editing = item }) { Text("Edit") }
                        }
                        Text(item.optString("content"), style = MaterialTheme.typography.bodySmall, maxLines = 5)
                        TextButton(onClick = { viewModel.deleteInformation(item.optInt("id")) }) { Text("Delete") }
                    }
                }
            }
        }
    }
    if (creating || editing != null) {
        InfoDialog(editing, { creating = false; editing = null }) { id, title, content, enabled ->
            creating = false; editing = null; viewModel.saveInformation(id, title, content, enabled)
        }
    }
}

@Composable
private fun InfoDialog(existing: JSONObject?, onDismiss: () -> Unit, onSave: (Int?, String, String, Boolean) -> Unit) {
    var title by remember(existing) { mutableStateOf(existing?.optString("title") ?: "") }
    var content by remember(existing) { mutableStateOf(existing?.optString("content") ?: "") }
    var enabled by remember(existing) { mutableStateOf(existing?.optBoolean("enabled", true) ?: true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add information" else "Edit information") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it.take(120) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(content, { content = it }, label = { Text("Content") }, minLines = 6, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(enabled, { enabled = it }); Text("Enabled") }
        } },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank() && content.isNotBlank()) onSave(existing?.optInt("id")?.takeIf { it > 0 }, title, content, enabled) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
                    OutlinedButton(onClick = { viewModel.queueAction("clear") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Clear queue") }
                    state.queueItems.forEach { item ->
                        HorizontalDivider(Modifier.padding(vertical = 7.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.optString("title", item.optString("script_title", "Queued script")), modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.moveQueueItem(item.optInt("id"), -1) }) { Text("↑") }
                            TextButton(onClick = { viewModel.moveQueueItem(item.optInt("id"), 1) }) { Text("↓") }
                            TextButton(onClick = { viewModel.removeQueueItem(item.optInt("id")) }) { Text("Remove") }
                        }
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
        ScriptDialog(editing, { creating = false; editing = null }) { id, payload -> creating = false; editing = null; viewModel.saveScript(id, payload) }
    }
}

@Composable
private fun ScriptDialog(existing: JSONObject?, onDismiss: () -> Unit, onSave: (Int?, JSONObject) -> Unit) {
    var title by remember(existing) { mutableStateOf(existing?.optString("title") ?: "") }
    var text by remember(existing) { mutableStateOf(existing?.optString("text") ?: "") }
    var enabled by remember(existing) { mutableStateOf(existing?.optBoolean("enabled", true) ?: true) }
    var language by remember(existing) { mutableStateOf(existing?.optString("language", "en") ?: "en") }
    var ttsMode by remember(existing) { mutableStateOf(existing?.optString("tts_mode", "edge") ?: "edge") }
    var edgeVoice by remember(existing) { mutableStateOf(existing?.optString("edge_voice", "en-US-AriaNeural") ?: "en-US-AriaNeural") }
    var kokoroId by remember(existing) { mutableStateOf(existing?.optInt("kokoro_voice_id", 0)?.toString() ?: "0") }
    var rate by remember(existing) { mutableStateOf(existing?.optDouble("tts_rate", 1.0)?.toString() ?: "1.0") }
    var volume by remember(existing) { mutableStateOf(existing?.optDouble("tts_volume", 1.0)?.toString() ?: "1.0") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create script" else "Edit script") },
        text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(title, { title = it.take(120) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(text, { text = it }, label = { Text("Script text") }, minLines = 5, modifier = Modifier.fillMaxWidth()) }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(enabled, { enabled = it }); Text("Enabled") } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(language, { language = it.take(2).lowercase() }, label = { Text("Language") }, modifier = Modifier.weight(1f))
                OutlinedTextField(ttsMode, { ttsMode = it }, label = { Text("TTS mode") }, modifier = Modifier.weight(1f))
            } }
            item { OutlinedTextField(edgeVoice, { edgeVoice = it }, label = { Text("Edge voice") }, modifier = Modifier.fillMaxWidth()) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(kokoroId, { kokoroId = it.filter(Char::isDigit) }, label = { Text("Kokoro ID") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(rate, { rate = it }, label = { Text("Rate") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(volume, { volume = it }, label = { Text("Volume") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            } }
        } },
        confirmButton = { TextButton(onClick = {
            if (title.isBlank() || text.isBlank()) return@TextButton
            val payload = JSONObject().put("title", title.trim()).put("text", text.trim()).put("enabled", enabled)
                .put("language", if (language == "id") "id" else "en").put("tts_mode", ttsMode.ifBlank { "edge" })
                .put("edge_voice", edgeVoice.ifBlank { "en-US-AriaNeural" }).put("kokoro_voice_id", kokoroId.toIntOrNull() ?: 0)
                .put("tts_rate", rate.toDoubleOrNull() ?: 1.0).put("tts_volume", volume.toDoubleOrNull() ?: 1.0)
            onSave(existing?.optInt("id")?.takeIf { it > 0 }, payload)
        }) { Text("Save") } },
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
