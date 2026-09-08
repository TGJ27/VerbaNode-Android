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
import com.verbanode.mobile.BuildConfig
import com.verbanode.mobile.MainActivity
import com.verbanode.mobile.diagnostics.DiagnosticHealth
import com.verbanode.mobile.diagnostics.abbreviateFingerprint
import com.verbanode.mobile.diagnostics.compatibilityHealth
import com.verbanode.mobile.diagnostics.diagnosticHealth
import com.verbanode.mobile.diagnostics.includeDiagnosticLog
import com.verbanode.mobile.diagnostics.safeDiagnosticMessage
import com.verbanode.mobile.network.AndroidCoreContract
import com.verbanode.mobile.network.TrustedDevice
import org.json.JSONArray
import org.json.JSONObject

private fun JSONArray.objectList(): List<JSONObject> = buildList {
    for (index in 0 until length()) optJSONObject(index)?.let(::add)
}

@Composable
internal fun SettingsScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    var section by remember { mutableStateOf("conversation") }
    ManagementSubpage(viewModel, "Settings") { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Feedback(viewModel) }
            item {
                DashboardCard("Settings categories", "Same Core settings used by the web dashboard.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (section == "conversation") Button({ section = "conversation" }, Modifier.weight(1f)) { Text("Conversation") }
                        else OutlinedButton({ section = "conversation" }, Modifier.weight(1f)) { Text("Conversation") }
                        if (section == "audio") Button({ section = "audio" }, Modifier.weight(1f)) { Text("Audio") }
                        else OutlinedButton({ section = "audio" }, Modifier.weight(1f)) { Text("Audio") }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (section == "models") Button({ section = "models" }, Modifier.weight(1f)) { Text("AI / Models") }
                        else OutlinedButton({ section = "models" }, Modifier.weight(1f)) { Text("AI / Models") }
                        if (section == "runtime") Button({ section = "runtime" }, Modifier.weight(1f)) { Text("Runtime") }
                        else OutlinedButton({ section = "runtime" }, Modifier.weight(1f)) { Text("Runtime") }
                    }
                }
            }
            when (section) {
                "conversation" -> item { ConversationSettingsCard(viewModel, state.runtimeSettings, state.audioDevices) }
                "audio" -> item { AudioSettingsCard(viewModel, state.runtimeSettings, state.audioDevices) }
                "models" -> item { ModelsCard(viewModel, state.modelItems) }
                else -> item { RuntimeCard(viewModel, state.dashboardStatus) }
            }
        }
    }
}

@Composable
private fun ConversationSettingsCard(viewModel: AppViewModel, settings: JSONObject?, audioDevices: JSONObject?) {
    val key = settings?.toString().orEmpty()
    var interruption by remember(key) { mutableStateOf(settings?.optBoolean("interruption_enabled", false) ?: false) }
    var silence by remember(key) { mutableStateOf((settings?.optInt("silence_ms", 900) ?: 900).toString()) }
    var maxRecord by remember(key) { mutableStateOf((settings?.optInt("max_record_seconds", 30) ?: 30).toString()) }
    var filter by remember(key) { mutableStateOf(settings?.optBoolean("stt_confidence_filter_enabled", true) ?: true) }
    var threshold by remember(key) { mutableStateOf((settings?.optDouble("stt_confidence_threshold", 0.70) ?: 0.70).toString()) }
    var rejected by remember(key) { mutableStateOf(settings?.optBoolean("show_rejected_stt_transcripts", true) ?: true) }
    DashboardCard("Conversation", "Recording, interruption and STT confidence behavior.") {
        ToggleRow("Allow interruption", interruption) { interruption = it }
        ToggleRow("STT confidence filter", filter) { filter = it }
        ToggleRow("Show rejected transcripts", rejected) { rejected = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(silence, { silence = it.filter(Char::isDigit) }, label = { Text("Silence ms") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(maxRecord, { maxRecord = it.filter(Char::isDigit) }, label = { Text("Max seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        OutlinedTextField(threshold, { threshold = it }, label = { Text("STT confidence 0–1") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(
            onClick = {
                val payload = JSONObject()
                    .put("interruption_enabled", interruption)
                    .put("silence_ms", (silence.toIntOrNull() ?: 900).coerceIn(300, 5000))
                    .put("max_record_seconds", (maxRecord.toIntOrNull() ?: 30).coerceIn(3, 180))
                    .put("stt_confidence_filter_enabled", filter)
                    .put("stt_confidence_threshold", (threshold.toDoubleOrNull() ?: 0.70).coerceIn(0.0, 1.0))
                    .put("show_rejected_stt_transcripts", rejected)
                    .put("input_device", settings?.opt("input_device") ?: JSONObject.NULL)
                    .put("output_device", settings?.opt("output_device") ?: JSONObject.NULL)
                viewModel.saveConversationSettings(payload)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) { Text("Save conversation settings") }
    }
}

@Composable
private fun AudioSettingsCard(viewModel: AppViewModel, settings: JSONObject?, audioDevices: JSONObject?) {
    val inputs = (audioDevices?.optJSONArray("inputs") ?: JSONArray()).objectList()
    val outputs = (audioDevices?.optJSONArray("outputs") ?: JSONArray()).objectList()
    val currentIn = if (settings != null && settings.has("input_device") && !settings.isNull("input_device")) settings.optInt("input_device") else null
    val currentOut = if (settings != null && settings.has("output_device") && !settings.isNull("output_device")) settings.optInt("output_device") else null
    var inputId by remember(settings?.toString(), audioDevices?.toString()) { mutableStateOf(currentIn) }
    var outputId by remember(settings?.toString(), audioDevices?.toString()) { mutableStateOf(currentOut) }
    DashboardCard("Host audio", "Select the Windows microphone/speaker and run the same device tests as the web dashboard.") {
        Text("Microphones", fontWeight = FontWeight.Bold)
        AudioDefaultRow(
            label = "Windows Default",
            detail = "Let Windows choose the current default microphone",
            selected = inputId == null,
            onSelect = { inputId = null },
        )
        if (inputs.isEmpty()) Text("No specific input devices reported.", style = MaterialTheme.typography.bodySmall)
        inputs.forEach { item ->
            val id = item.optInt("id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = inputId == id, onCheckedChange = { if (it) inputId = id })
                Column(Modifier.weight(1f)) {
                    Text(item.optString("name", "Input $id"), style = MaterialTheme.typography.bodySmall)
                    Text(
                        buildList {
                            item.optString("hostapi").takeIf { it.isNotBlank() }?.let(::add)
                            if (item.optBoolean("is_default_input", false)) add("Windows default")
                            if (item.optBoolean("recommended_input", false)) add("Recommended")
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Speakers", fontWeight = FontWeight.Bold)
        AudioDefaultRow(
            label = "Windows Default",
            detail = "Let Windows choose the current default speaker",
            selected = outputId == null,
            onSelect = { outputId = null },
        )
        if (outputs.isEmpty()) Text("No specific output devices reported.", style = MaterialTheme.typography.bodySmall)
        outputs.forEach { item ->
            val id = item.optInt("id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = outputId == id, onCheckedChange = { if (it) outputId = id })
                Column(Modifier.weight(1f)) {
                    Text(item.optString("name", "Output $id"), style = MaterialTheme.typography.bodySmall)
                    Text(
                        buildList {
                            item.optString("hostapi").takeIf { it.isNotBlank() }?.let(::add)
                            if (item.optBoolean("is_default_output", false)) add("Windows default")
                            if (item.optBoolean("recommended_output", false)) add("Recommended")
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = viewModel::refreshAudioDevices, modifier = Modifier.weight(1f)) { Text("Refresh") }
            OutlinedButton(onClick = { viewModel.testAudio("test-input", inputId, outputId) }, modifier = Modifier.weight(1f)) { Text("Test mic") }
            OutlinedButton(onClick = { viewModel.testAudio("test-output", inputId, outputId) }, modifier = Modifier.weight(1f)) { Text("Test speaker") }
        }
        Button(
            onClick = {
                val payload = JSONObject()
                    .put("interruption_enabled", settings?.optBoolean("interruption_enabled", false) ?: false)
                    .put("silence_ms", settings?.optInt("silence_ms", 900) ?: 900)
                    .put("max_record_seconds", settings?.optInt("max_record_seconds", 30) ?: 30)
                    .put("stt_confidence_filter_enabled", settings?.optBoolean("stt_confidence_filter_enabled", true) ?: true)
                    .put("stt_confidence_threshold", settings?.optDouble("stt_confidence_threshold", 0.70) ?: 0.70)
                    .put("show_rejected_stt_transcripts", settings?.optBoolean("show_rejected_stt_transcripts", true) ?: true)
                    .put("input_device", inputId ?: JSONObject.NULL)
                    .put("output_device", outputId ?: JSONObject.NULL)
                viewModel.saveConversationSettings(payload)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Save audio devices") }
    }
}

@Composable
private fun AudioDefaultRow(
    label: String,
    detail: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = { if (it) onSelect() })
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModelsCard(viewModel: AppViewModel, models: List<JSONObject>) {
    var pullName by remember { mutableStateOf("") }
    DashboardCard("AI models", "Manage Ollama models and reload the AI components.") {
        if (models.isEmpty()) Text("No models reported or Ollama is unavailable.", style = MaterialTheme.typography.bodySmall)
        models.forEach { model ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(model.optString("name", model.optString("model", "model")), fontWeight = FontWeight.SemiBold)
                    val size = model.optString("size").ifBlank { model.optLong("size", 0L).takeIf { it > 0 }?.toString().orEmpty() }
                    if (size.isNotBlank()) Text(size, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        OutlinedTextField(pullName, { pullName = it }, label = { Text("Pull model (example qwen3.5:0.8b)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(onClick = { if (pullName.isNotBlank()) viewModel.pullModel(pullName) }, enabled = pullName.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Pull model") }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = viewModel::restartAiEngine, modifier = Modifier.weight(1f)) { Text("Restart AI") }
            OutlinedButton(onClick = viewModel::reloadAsr, modifier = Modifier.weight(1f)) { Text("Reload ASR") }
            OutlinedButton(onClick = viewModel::reloadKokoro, modifier = Modifier.weight(1f)) { Text("Reload TTS") }
        }
    }
}

@Composable
private fun RuntimeCard(viewModel: AppViewModel, raw: JSONObject?) {
    DashboardCard("Runtime", "Inspect and restart Core-managed worker engines.") {
        val ai = raw?.optJSONObject("ai") ?: JSONObject()
        val audio = raw?.optJSONObject("audio") ?: JSONObject()
        Text("AI: ${ai.optString("mode", "unknown")}")
        Text("Audio: ${audio.optString("mode", "unknown")}")
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::restartAiEngine, modifier = Modifier.weight(1f)) { Text("Restart AI") }
            Button(onClick = viewModel::restartAudioEngine, modifier = Modifier.weight(1f)) { Text("Restart Audio") }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun DevicesScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    var renameTarget by remember { mutableStateOf<TrustedDevice?>(null) }
    ManagementSubpage(viewModel, "Trusted devices") { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Feedback(viewModel) }
            item {
                DashboardCard("Pair new device", "Open a short pairing window from this controller. Another Android device can enter the code.") {
                    Button(onClick = viewModel::startPairingManagement, modifier = Modifier.fillMaxWidth()) { Text("Start pairing") }
                    state.pairingStatus?.let { pairing ->
                        Text("Pairing code", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 10.dp))
                        Text(pairing.optString("short_code", pairing.optString("code", "")), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Expires: ${pairing.optString("expires_at", "soon")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(state.devices, key = { it.deviceId }) { device ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.Bold)
                                Text("${device.deviceType} · ${if (device.trusted) "Trusted" else "Revoked"}${if (device.activeController) " · Active" else ""}", style = MaterialTheme.typography.bodySmall)
                                device.lastSeenAt?.let { Text("Last seen: $it", style = MaterialTheme.typography.labelSmall) }
                            }
                            TextButton(onClick = { renameTarget = device }) { Text("Rename") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (device.trusted) OutlinedButton(onClick = { viewModel.revokeDevice(device.deviceId) }) { Text("Revoke") }
                            else OutlinedButton(onClick = { viewModel.deleteDevice(device.deviceId) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
    renameTarget?.let { device -> RenameDeviceDialog(device, { renameTarget = null }) { name -> renameTarget = null; viewModel.renameDevice(device.deviceId, name) } }
}

@Composable
private fun RenameDeviceDialog(device: TrustedDevice, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(device.deviceId) { mutableStateOf(device.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename device") },
        text = { OutlinedTextField(name, { name = it.take(120) }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun DiagnosticsScreen(viewModel: AppViewModel, activity: MainActivity) {
    val state by viewModel.ui.collectAsState()
    val diagnostics = state.diagnosticsStatus ?: JSONObject()
    val snapshot = diagnostics.optJSONObject("snapshot") ?: JSONObject()
    val compatibility = snapshot.optJSONObject("compatibility") ?: JSONObject()
    val selfTest = diagnostics.optJSONObject("self_test")
    val profile = state.currentProfile
    val clientInfo = state.clientInfo
    var warningOnly by remember { mutableStateOf(true) }
    var confirmClearLogs by remember { mutableStateOf(false) }
    var showRawSnapshot by remember { mutableStateOf(false) }

    val remoteFingerprint = compatibility.optString("mobile_contract_fingerprint").ifBlank {
        clientInfo?.mobileContractFingerprint.orEmpty()
    }
    val compatibilityHealth = compatibilityHealth(remoteFingerprint, AndroidCoreContract.EXPECTED_FINGERPRINT)
    val visibleLogs = state.diagnosticsLogs.filter {
        includeDiagnosticLog(it.optString("level"), warningOnly)
    }.takeLast(80).reversed()

    fun healthText(value: DiagnosticHealth): String = when (value) {
        DiagnosticHealth.GOOD -> "Healthy"
        DiagnosticHealth.WARN -> "Warning"
        DiagnosticHealth.BAD -> "Problem"
        DiagnosticHealth.UNKNOWN -> "Unknown"
    }

    ManagementSubpage(viewModel, "Diagnostics") { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Feedback(viewModel) }
            item {
                DashboardCard("Compatibility", "Mobile/Core protocol compatibility checked before credentials are used.") {
                    Text("Android v${BuildConfig.VERSION_NAME} · Core ${clientInfo?.serverVersion ?: profile?.lastServerVersion ?: compatibility.optString("server_version", "unknown")}", fontWeight = FontWeight.Bold)
                    Text("API v${clientInfo?.apiVersion ?: compatibility.optInt("api_version", 0)} · WebSocket v${clientInfo?.websocketVersion ?: compatibility.optInt("websocket_protocol_version", 0)} · Mobile contract v${clientInfo?.mobileContract?.contractVersion ?: compatibility.optInt("mobile_contract_version", 0)}")
                    Text("Contract: ${healthText(compatibilityHealth)} · ${abbreviateFingerprint(remoteFingerprint)}", modifier = Modifier.padding(top = 4.dp))
                    if (compatibilityHealth == DiagnosticHealth.BAD) {
                        Text("The Android and Core contract fingerprints differ. Update both components together before continuing.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            item {
                DashboardCard("Connection & trust", "Only the public TLS SPKI fingerprint is shown; credentials are never included.") {
                    Text(profile?.name ?: clientInfo?.instanceName ?: "VerbaNode", fontWeight = FontWeight.Bold)
                    Text(profile?.baseUrl ?: "No active server")
                    Text("Connection: ${state.connectionLabel}")
                    Text("TLS SPKI: ${abbreviateFingerprint(profile?.spkiSha256 ?: clientInfo?.certificateSpkiSha256)}")
                    Text("Transport: LAN HTTPS/WSS · Cloud disabled", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                val audio = snapshot.optJSONObject("audio")?.optJSONObject("engine")
                val ai = snapshot.optJSONObject("ai")?.optJSONObject("engine")
                val pipeline = snapshot.optJSONObject("pipeline") ?: JSONObject()
                val audioHealth = diagnosticHealth(audio?.takeIf { it.has("alive") }?.optBoolean("alive"), audio?.optString("error")?.ifBlank { null })
                val aiHealth = diagnosticHealth(ai?.takeIf { it.has("alive") }?.optBoolean("alive"), ai?.optString("error")?.ifBlank { null })
                DashboardCard("Core health", "Live health from the authenticated diagnostics API.") {
                    Text("Audio engine: ${healthText(audioHealth)}")
                    Text("AI engine: ${healthText(aiHealth)}")
                    Text("Pipeline: ${pipeline.optString("state", snapshot.optString("mode", "unknown"))}")
                    Text("Queue: ${snapshot.optString("queue_state", "unknown")}")
                    val generated = snapshot.optString("generated_at")
                    if (generated.isNotBlank()) Text("Snapshot: $generated", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = viewModel::runDiagnosticsSelfTest, modifier = Modifier.weight(1f)) { Text("Run self-test") }
                        OutlinedButton(onClick = viewModel::openDiagnostics, modifier = Modifier.weight(1f)) { Text("Refresh") }
                    }
                }
            }
            if (selfTest != null) item {
                val checks = (selfTest.optJSONArray("checks") ?: JSONArray()).objectList()
                DashboardCard("Last self-test", "${selfTest.optInt("failures", 0)} failures · ${selfTest.optInt("warnings", 0)} warnings") {
                    Text("Overall: ${selfTest.optString("overall", "unknown").uppercase()}", fontWeight = FontWeight.Bold)
                    checks.forEach { check ->
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Text("${check.optString("name", "Check")}: ${check.optString("status", "unknown").uppercase()}", fontWeight = FontWeight.SemiBold)
                        Text(check.optString("detail").take(500), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                DashboardCard("Recent sanitized logs", "Core redacts credentials and Android applies a second display-time redaction pass.") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = warningOnly, onCheckedChange = { warningOnly = it })
                        Text("Warnings and errors only")
                    }
                    if (visibleLogs.isEmpty()) {
                        Text("No matching diagnostic log entries.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        visibleLogs.take(20).forEach { entry ->
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            Text("${entry.optString("level", "INFO")} · ${entry.optString("logger", "core")}", fontWeight = FontWeight.SemiBold)
                            Text(safeDiagnosticMessage(entry.optString("message")).take(800), style = MaterialTheme.typography.bodySmall)
                            val timestamp = entry.optString("timestamp")
                            if (timestamp.isNotBlank()) Text(timestamp, style = MaterialTheme.typography.labelSmall)
                        }
                        if (visibleLogs.size > 20) Text("Showing newest 20 of ${visibleLogs.size} matching entries.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                    }
                    OutlinedButton(onClick = { confirmClearLogs = true }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Clear diagnostic logs") }
                }
            }
            item {
                DashboardCard("Diagnostics export", "The ZIP is generated by Core and excludes PINs, session tokens, databases, conversations, certificates/private keys, and model files.") {
                    Button(
                        onClick = { viewModel.exportDiagnostics { bytes, name, mime -> activity.saveDocument(bytes, name, mime) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Export sanitized diagnostics ZIP") }
                    OutlinedButton(onClick = { showRawSnapshot = !showRawSnapshot }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(if (showRawSnapshot) "Hide raw health snapshot" else "Show raw health snapshot")
                    }
                    if (showRawSnapshot) Text(prettyJson(snapshot), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    if (confirmClearLogs) {
        AlertDialog(
            onDismissRequest = { confirmClearLogs = false },
            title = { Text("Clear diagnostic logs?") },
            text = { Text("This clears Core's in-memory sanitized diagnostic log buffer. It does not delete conversations, Knowledge, agents, or Windows log files.") },
            confirmButton = { TextButton(onClick = { confirmClearLogs = false; viewModel.clearDiagnosticLogs() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClearLogs = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun DataScreen(viewModel: AppViewModel, activity: MainActivity) {
    val state by viewModel.ui.collectAsState()
    val status = state.backupStatus ?: JSONObject()
    ManagementSubpage(viewModel, "Data & Recovery") { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Feedback(viewModel) }
            item {
                DashboardCard("Backup status", "VerbaNode's SQLite backup/recovery layer remains authoritative; the phone only transfers the archive.") {
                    Text("Backup format: ${status.optInt("format_version", 0)}")
                    Text("Database schema: ${status.optInt("schema_version", 0)} / ${status.optInt("current_schema_version", 0)}")
                    val recovery = status.optJSONArray("recovery_backups") ?: JSONArray()
                    Text("Recovery snapshots: ${recovery.length()}")
                }
            }
            item {
                DashboardCard("Backup & restore", "Restore replaces Core data and may require restarting VerbaNode.") {
                    Button(
                        onClick = { viewModel.exportBackup { bytes, name, mime -> activity.saveDocument(bytes, name, mime) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Download full backup") }
                    OutlinedButton(onClick = activity::chooseBackupForRestore, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Restore from backup ZIP") }
                }
            }
        }
    }
}

@Composable
internal fun StatusScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    ManagementSubpage(viewModel, "About / Status") { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Feedback(viewModel) }
            item {
                DashboardCard("VerbaNode") {
                    Text("Android controller v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                    Text("Core ${state.clientInfo?.serverVersion ?: state.currentProfile?.lastServerVersion ?: "unknown"}")
                    Text("API v${state.clientInfo?.apiVersion ?: 1} · WebSocket v${state.clientInfo?.websocketVersion ?: 1}")
                    Text("Connection: ${state.currentProfile?.baseUrl.orEmpty()}")
                    Text("Transport: LAN HTTPS/WSS only")
                    Text("Cloud access: disabled")
                }
            }
            item {
                DashboardCard("Runtime summary") {
                    Text(state.statusText.ifBlank { "Open Dashboard to refresh the current runtime status." }, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = viewModel::openStatus, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Refresh status") }
                }
            }
        }
    }
}

private fun prettyJson(value: JSONObject): String = runCatching { value.toString(2) }.getOrElse { value.toString() }.take(8000)
