package com.verbanode.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.verbanode.mobile.AppViewModel
import org.json.JSONArray
import org.json.JSONObject

private fun tttChoices(config: JSONObject?, key: String): List<Pair<String, String>> {
    val array = config?.optJSONArray(key) ?: JSONArray()
    val loaded = buildList {
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is JSONObject -> add(item.optString("value") to item.optString("label", item.optString("value")))
                is String -> add(item to item)
            }
        }
    }.filter { it.first.isNotBlank() }
    if (loaded.isNotEmpty()) return loaded
    return when (key) {
        "languages" -> listOf("en" to "English", "id" to "Bahasa Indonesia")
        "tts_modes" -> listOf(
            "edge" to "Edge only",
            "kokoro" to "Kokoro local only",
            "edge_fallback" to "Edge → Kokoro fallback",
            "kokoro_fallback" to "Kokoro → Edge fallback",
        )
        else -> emptyList()
    }
}

private fun tttEdgeVoiceChoices(config: JSONObject?, language: String): List<Pair<String, String>> {
    val array = config?.optJSONArray("edge_voices") ?: JSONArray()
    val prefix = if (language == "id") "id-" else "en-"
    return buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val shortName = item.optString("short_name").trim()
            if (shortName.isBlank() || !shortName.lowercase().startsWith(prefix)) continue
            val name = item.optString("name", shortName).trim().ifBlank { shortName }
            val locale = item.optString("locale").trim()
            val gender = item.optString("gender").trim()
            val details = listOf(locale, gender).filter { it.isNotBlank() }.joinToString(" · ")
            add(shortName to if (details.isBlank()) name else "$name · $details")
        }
    }
}

@Composable
internal fun TypeToTalkScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    var text by remember { mutableStateOf("") }
    var ttsSettingsExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val settings = state.typeToTalkSettings ?: JSONObject()
    val settingsKey = settings.toString()
    var language by remember(settingsKey) { mutableStateOf(settings.optString("language", "en")) }
    var ttsMode by remember(settingsKey) { mutableStateOf(settings.optString("tts_mode", "edge")) }
    var edgeVoice by remember(settingsKey) { mutableStateOf(settings.optString("edge_voice", "en-US-AriaNeural")) }
    var kokoroId by remember(settingsKey) { mutableStateOf(settings.optInt("kokoro_voice_id", 0).toString()) }
    var rate by remember(settingsKey) { mutableStateOf(settings.optDouble("tts_rate", 1.0).toString()) }
    var volume by remember(settingsKey) { mutableStateOf(settings.optDouble("tts_volume", 1.0).toString()) }

    fun settingsPayload(): JSONObject {
        val normalizedLanguage = if (language == "id") "id" else "en"
        val normalizedMode = if (normalizedLanguage == "id") "edge" else ttsMode.ifBlank { "edge" }
        val defaultVoice = if (normalizedLanguage == "id") "id-ID-GadisNeural" else "en-US-AriaNeural"
        val normalizedVoice = when {
            normalizedLanguage == "id" && !edgeVoice.startsWith("id-") -> defaultVoice
            normalizedLanguage == "en" && edgeVoice.startsWith("id-") -> defaultVoice
            else -> edgeVoice.ifBlank { defaultVoice }
        }
        return JSONObject()
            .put("language", normalizedLanguage)
            .put("tts_mode", normalizedMode)
            .put("edge_voice", normalizedVoice)
            .put("kokoro_voice_id", kokoroId.toIntOrNull() ?: 0)
            .put("tts_rate", (rate.toDoubleOrNull() ?: 1.0).coerceIn(0.5, 2.0))
            .put("tts_volume", (volume.toDoubleOrNull() ?: 1.0).coerceIn(0.0, 1.0))
    }

    fun saveSettings() = viewModel.saveTypeToTalkSettings(settingsPayload())

    fun send() {
        val value = text.trim()
        if (value.isBlank()) return
        val payload = settingsPayload()
        text = ""
        viewModel.addTypeToTalk(value, payload)
    }

    LaunchedEffect(state.typeToTalkItems.size) {
        if (state.typeToTalkItems.isNotEmpty()) listState.animateScrollToItem(state.typeToTalkItems.lastIndex)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::openHome) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                    Text(
                        "Type to Talk",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.size(48.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Feedback(viewModel)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.AutoMirrored.Outlined.VolumeUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(7.dp).size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("TTS / Voice", fontWeight = FontWeight.Bold)
                            Text(
                                "$language · ${ttsMode.ifBlank { "edge" }} · $edgeVoice",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        TextButton(onClick = { ttsSettingsExpanded = !ttsSettingsExpanded }) {
                            Text(if (ttsSettingsExpanded) "Hide" else "Configure")
                        }
                    }
                    if (ttsSettingsExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceField("Language", language, tttChoices(state.configurationOptions, "languages"), Modifier.weight(1f)) { selected ->
                                language = selected
                                if (language == "id") {
                                    ttsMode = "edge"
                                    if (!edgeVoice.startsWith("id-")) edgeVoice = "id-ID-GadisNeural"
                                } else if (edgeVoice.startsWith("id-")) edgeVoice = "en-US-AriaNeural"
                            }
                            ChoiceField("TTS mode", ttsMode, tttChoices(state.configurationOptions, "tts_modes"), Modifier.weight(1f)) { selected ->
                                if (language != "id" || selected == "edge") ttsMode = selected
                            }
                        }
                        ChoiceField("Voice", edgeVoice, tttEdgeVoiceChoices(state.configurationOptions, language), Modifier.fillMaxWidth()) { edgeVoice = it }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                kokoroId,
                                { kokoroId = it.filter(Char::isDigit) },
                                label = { Text("Kokoro ID") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                enabled = language != "id",
                            )
                            OutlinedTextField(rate, { rate = it }, label = { Text("Rate") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(volume, { volume = it }, label = { Text("Volume") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                        }
                        OutlinedButton(onClick = ::saveSettings, modifier = Modifier.fillMaxWidth()) { Text("Save TTS settings") }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(20000) },
                        placeholder = { Text("Type what VerbaNode should say…") },
                        modifier = Modifier.fillMaxWidth().height(132.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${text.length}/20000", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = ::send, enabled = text.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send")
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Speech queue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    when (state.typeToTalkState.lowercase()) {
                        "playing" -> "Speaking"
                        "paused" -> "Paused"
                        else -> "Ready"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.typeToTalkItems.isEmpty()) item {
                        Text(
                            "Nothing queued yet. Type text above and press Send.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    itemsIndexed(state.typeToTalkItems, key = { _, item -> item.optInt("id") }) { index, item ->
                        val id = item.optInt("id")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (item.optString("status") == "playing") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                                Text(item.optString("text"), style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (item.optString("status") == "playing") "Speaking now" else "Queued ${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { viewModel.removeTypeToTalk(id) }) { Text("Remove") }
                                }
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.typeToTalkAction("play") }, modifier = Modifier.weight(1f)) { Text("Play") }
                OutlinedButton(onClick = { viewModel.typeToTalkAction("stop") }, modifier = Modifier.weight(1f)) { Text("Stop") }
                OutlinedButton(onClick = { viewModel.typeToTalkAction("clear") }, modifier = Modifier.weight(1f)) { Text("Clear") }
            }
        }
    }
}
