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
    ManagementScaffold(viewModel, "Audio Library", AppScreen.AUDIO) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Feedback(viewModel) }
            item {
                DashboardCard("Host audio", "Upload common audio formats and play them through the VerbaNode Windows host.") {
                    Button(onClick = activity::chooseAudioForUpload, modifier = Modifier.fillMaxWidth()) { Text("＋ Upload audio") }
                    OutlinedButton(onClick = viewModel::stopAudio, modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) { Text("Stop playback") }
                    Text(
                        state.audioLibraryPlaying?.let { "Playing: $it" } ?: "No audio playing",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            if (state.audioLibraryItems.isEmpty()) item {
                Text("No uploaded audio yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.audioLibraryItems, key = { it.optString("name") }) { item ->
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
                            Button(onClick = { viewModel.playAudio(name) }, modifier = Modifier.weight(1f)) { Text(if (playing) "Restart" else "Play") }
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
