package com.verbanode.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verbanode.mobile.AppScreen
import com.verbanode.mobile.AppViewModel
import com.verbanode.mobile.MainActivity
import org.json.JSONObject

@Composable
internal fun AudioLibraryScreen(viewModel: AppViewModel, activity: MainActivity) {
    val state by viewModel.ui.collectAsState()
    var renameTarget by remember { mutableStateOf<JSONObject?>(null) }
    var sourceFilter by remember { mutableStateOf("all") }
    ManagementScaffold(viewModel, "Audio Library", AppScreen.AUDIO) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Feedback(viewModel) }
            item {
                Button(onClick = activity::chooseAudioForUpload, modifier = Modifier.fillMaxWidth()) { Text("＋ Upload Audio") }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sourceFilter == "all") Button(onClick = { sourceFilter = "all" }, modifier = Modifier.weight(1f)) { Text("All") }
                    else OutlinedButton(onClick = { sourceFilter = "all" }, modifier = Modifier.weight(1f)) { Text("All") }
                    if (sourceFilter == "uploaded") Button(onClick = { sourceFilter = "uploaded" }, modifier = Modifier.weight(1f)) { Text("Uploaded") }
                    else OutlinedButton(onClick = { sourceFilter = "uploaded" }, modifier = Modifier.weight(1f)) { Text("Uploaded") }
                    if (sourceFilter == "generated") Button(onClick = { sourceFilter = "generated" }, modifier = Modifier.weight(1f)) { Text("Generated") }
                    else OutlinedButton(onClick = { sourceFilter = "generated" }, modifier = Modifier.weight(1f)) { Text("Generated") }
                }
                Text(
                    state.audioLibraryPlaying?.let { "Playing: $it" } ?: "No audio playing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (state.audioLibraryPlaying != null) {
                    OutlinedButton(onClick = viewModel::stopAudio, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Stop playback") }
                }
            }
            val visibleAudio = state.audioLibraryItems.filter { item ->
                val source = listOf(item.optString("source"), item.optString("source_type"), item.optString("kind")).joinToString(" ").lowercase()
                when (sourceFilter) {
                    "uploaded" -> source.isBlank() || source.contains("upload") || source.contains("file")
                    "generated" -> source.contains("generated") || source.contains("tts")
                    else -> true
                }
            }
            if (visibleAudio.isEmpty()) item {
                Text("No audio files in this view.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(visibleAudio, key = { it.optString("name") }) { item ->
                val name = item.optString("name")
                val playing = state.audioLibraryPlaying == name || item.optBoolean("playing")
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (playing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(name, fontWeight = FontWeight.Bold)
                        val duration = item.optDouble("duration_seconds", -1.0)
                        val sizeKb = item.optLong("size_bytes", 0L) / 1024L
                        Text(
                            buildString {
                                append("${sizeKb} KB")
                                if (duration >= 0) append(" · ${"%.1f".format(duration)} sec")
                                if (playing) append(" · PLAYING")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { viewModel.playAudio(name) }, modifier = Modifier.weight(1f)) { Text(if (playing) "Replay" else "Play") }
                            OutlinedButton(onClick = { renameTarget = item }, modifier = Modifier.weight(1f)) { Text("Rename") }
                            OutlinedButton(onClick = { viewModel.deleteAudio(name) }, modifier = Modifier.weight(1f)) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
    renameTarget?.let { item ->
        var value by remember(item) { mutableStateOf(item.optString("name")) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename audio") },
            text = { OutlinedTextField(value, { value = it }, label = { Text("Filename") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val old = item.optString("name")
                    val newName = value.trim()
                    if (newName.isNotBlank()) { renameTarget = null; viewModel.renameAudio(old, newName) }
                }) { Text("Rename") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
        )
    }
}
