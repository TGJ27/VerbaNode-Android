package com.verbanode.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.verbanode.mobile.AppScreen
import com.verbanode.mobile.AppViewModel
import org.json.JSONArray
import org.json.JSONObject

private fun tttChoices(config: JSONObject?, key: String): List<Pair<String, String>> {
    val array = config?.optJSONArray(key) ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            val raw = array.opt(index)
            when (raw) {
                is JSONObject -> {
                    val value = raw.optString("value")
                    if (value.isNotBlank()) add(value to raw.optString("label", value))
                }
                is String -> if (raw.isNotBlank()) add(raw to raw)
            }
        }
    }
}

@Composable
internal fun TypeToTalkScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    val defaults = state.typeToTalkDefaults ?: JSONObject()
    val defaultsKey = defaults.toString()

    var text by remember { mutableStateOf("") }
    var language by remember(defaultsKey) { mutableStateOf(defaults.optString("language", "en")) }
    var ttsMode by remember(defaultsKey) { mutableStateOf(defaults.optString("tts_mode", "edge")) }
    var edgeVoice by remember(defaultsKey) { mutableStateOf(defaults.optString("edge_voice", "en-US-AriaNeural")) }
    var kokoroId by remember(defaultsKey) { mutableStateOf(defaults.optInt("kokoro_voice_id", 0).toString()) }
    var rate by remember(defaultsKey) { mutableStateOf(defaults.optDouble("tts_rate", 1.0).toString()) }
    var volume by remember(defaultsKey) { mutableStateOf(defaults.optDouble("tts_volume", 1.0).toString()) }

    fun send() {
        val value = text.trim()
        if (value.isBlank()) return
        val normalizedLanguage = if (language == "id") "id" else "en"
        val normalizedMode = if (normalizedLanguage == "id") "edge" else ttsMode.ifBlank { "edge" }
        val defaultVoice = if (normalizedLanguage == "id") "id-ID-GadisNeural" else "en-US-AriaNeural"
        val payload = JSONObject()
            .put("language", normalizedLanguage)
            .put("tts_mode", normalizedMode)
            .put("edge_voice", edgeVoice.ifBlank { defaultVoice })
            .put("kokoro_voice_id", kokoroId.toIntOrNull() ?: 0)
            .put("tts_rate", (rate.toDoubleOrNull() ?: 1.0).coerceIn(0.5, 2.0))
            .put("tts_volume", (volume.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 1.0))
        text = ""
        viewModel.addTypeToTalk(value, payload)
    }

    ManagementScaffold(viewModel, "Type to Talk", AppScreen.TYPE_TO_TALK) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Feedback(viewModel)

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceField(
                            "Language", language, tttChoices(state.configurationOptions, "languages"), Modifier.weight(1f)
                        ) { selected ->
                            language = selected
                            if (selected == "id") {
                                ttsMode = "edge"
                                if (!edgeVoice.startsWith("id-")) edgeVoice = "id-ID-GadisNeural"
                            } else if (edgeVoice.startsWith("id-")) edgeVoice = "en-US-AriaNeural"
                        }
                        ChoiceField(
                            "TTS mode", ttsMode, tttChoices(state.configurationOptions, "tts_modes"), Modifier.weight(1f)
                        ) { selected -> if (language != "id" || selected == "edge") ttsMode = selected }
                    }
                    OutlinedTextField(
                        edgeVoice,
                        { edgeVoice = it.take(120) },
                        label = { Text("Edge voice") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            kokoroId,
                            { kokoroId = it.filter(Char::isDigit).take(3) },
                            label = { Text("Kokoro ID") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = language != "id",
                        )
                        OutlinedTextField(
                            rate,
                            { rate = it.take(5) },
                            label = { Text("Rate") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            volume,
                            { volume = it.take(5) },
                            label = { Text("Volume") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${state.typeToTalkState.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = { viewModel.typeToTalkAction("play") }) { Text("Resume") }
                        OutlinedButton(onClick = { viewModel.typeToTalkAction("stop") }) { Text("Stop") }
                        TextButton(onClick = { viewModel.typeToTalkAction("clear") }) { Text("Clear") }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.typeToTalkItems.isEmpty()) {
                    item {
                        Text(
                            "Type below and tap Send. VerbaNode speaks it directly without the LLM.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
                items(state.typeToTalkItems, key = { item -> item.optInt("id") }) { item ->
                    val status = item.optString("status", "waiting")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Card(
                            modifier = Modifier.fillMaxWidth(0.88f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (status == "completed") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                Text(item.optString("text"), style = MaterialTheme.typography.bodyMedium)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        when (status) { "playing" -> "Speaking now"; "completed" -> "Spoken"; else -> "Queued" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = { viewModel.removeTypeToTalk(item.optInt("id")) }) { Text("Remove") }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(20000) },
                    placeholder = { Text("Type something to say…") },
                    modifier = Modifier.weight(1f),
                    minLines = 1,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                )
                Button(onClick = { send() }, enabled = text.isNotBlank()) { Text("Send") }
            }
        }
    }
}
