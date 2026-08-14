package com.verbanode.mobile.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.verbanode.mobile.AppScreen
import com.verbanode.mobile.AppViewModel
import com.verbanode.mobile.R
import com.verbanode.mobile.network.ChatMessage
import org.json.JSONObject

private val Success = Color(0xFF20B979)
private val SoftBlue = Color(0xFFF6F9FF)

@Composable
internal fun ManagementScaffold(
    viewModel: AppViewModel,
    title: String,
    selected: AppScreen,
    content: @Composable (PaddingValues) -> Unit,
) {
    val state by viewModel.ui.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.verbanode_logo),
                        contentDescription = "VerbaNode logo",
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("VerbaNode", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = viewModel::openSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                }
                Spacer(Modifier.height(10.dp))
                ServerStatusCard(viewModel)
                if (title.isNotBlank() && selected != AppScreen.HOME) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, start = 2.dp))
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(selected == AppScreen.HOME, viewModel::openHome, { Icon(Icons.Outlined.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected == AppScreen.CHAT, viewModel::openChat, { Icon(Icons.Outlined.Chat, null) }, label = { Text("Chat") })
                NavigationBarItem(selected == AppScreen.SCRIPTS, viewModel::openScripts, { Icon(Icons.Outlined.Description, null) }, label = { Text("Scripts") })
                NavigationBarItem(selected == AppScreen.MORE, { viewModel.navigate(AppScreen.MORE) }, { Icon(Icons.Outlined.MoreHoriz, null) }, label = { Text("More") })
            }
        },
        content = content,
    )
}

@Composable
private fun ServerStatusCard(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.Devices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(23.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(state.currentProfile?.name ?: "VerbaNode", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.connected) Success else MaterialTheme.colorScheme.error))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.connected) "Connected" else state.connectionLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Outlined.Wifi, null, tint = if (state.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("Core ${state.clientInfo?.serverVersion ?: state.currentProfile?.lastServerVersion ?: "?"}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text("API ${state.clientInfo?.apiVersion ?: 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun DashboardScreen(viewModel: AppViewModel, activity: Activity) {
    val state by viewModel.ui.collectAsState()
    val status = state.dashboardStatus ?: JSONObject()
    val ai = status.optJSONObject("ai") ?: JSONObject()
    val audio = status.optJSONObject("audio") ?: JSONObject()
    ManagementScaffold(viewModel, "", AppScreen.HOME) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Feedback(viewModel) }
            item { HeroCard(state.connected) }
            item {
                FeatureRow(
                    Feature(Icons.Outlined.Chat, "Chat", "Talk with your AI", viewModel::openChat),
                    Feature(Icons.Outlined.Person, "Agents", "Manage AI agents", viewModel::openAgents),
                )
            }
            item {
                FeatureRow(
                    Feature(Icons.Outlined.Extension, "Plugins", "Extend capabilities", viewModel::openPlugins),
                    Feature(Icons.Outlined.Devices, "Devices", "Trusted controllers", viewModel::openDevices),
                )
            }
            item {
                FeatureRow(
                    Feature(Icons.Outlined.MonitorHeart, "Diagnostics", "System health", viewModel::openDiagnostics),
                    Feature(Icons.Outlined.CloudUpload, "Backup", "Backup & restore", viewModel::openData),
                )
            }
            item { StatusStrip(ai.optString("mode", "ready"), audio.optString("mode", "ready"), state.connected) }
            item { ConversationControls(viewModel, activity) }
        }
    }
}

@Composable
private fun HeroCard(connected: Boolean) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Row(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))
            ).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (connected) "VerbaNode is online" else "VerbaNode is reconnecting", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (connected) "Your local AI system is ready to assist." else "Waiting for the local controller connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Image(painterResource(R.drawable.verbanode_logo), "VerbaNode", Modifier.size(82.dp).clip(RoundedCornerShape(20.dp)))
        }
    }
}

private data class Feature(val icon: ImageVector, val title: String, val subtitle: String, val action: () -> Unit)

@Composable
private fun FeatureRow(first: Feature, second: Feature) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FeatureCard(first, Modifier.weight(1f))
        FeatureCard(second, Modifier.weight(1f))
    }
}

@Composable
private fun FeatureCard(feature: Feature, modifier: Modifier) {
    Card(
        modifier = modifier.clickable(onClick = feature.action),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(feature.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(feature.title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Text(feature.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun StatusStrip(aiMode: String, audioMode: String, connected: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("System status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusItem("AI Engine", friendlyRuntimeMode(aiMode), Modifier.weight(1f))
                StatusItem("Audio Engine", friendlyRuntimeMode(audioMode), Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusItem("Core API", if (connected) "Online" else "Offline", Modifier.weight(1f))
                StatusItem("Trusted Device", if (connected) "Secure" else "Offline", Modifier.weight(1f))
            }
        }
    }
}

private fun friendlyRuntimeMode(raw: String): String {
    val value = raw.trim().lowercase()
    return when {
        value.isBlank() -> "Ready"
        value in setOf("offline", "error", "failed", "stopped", "unavailable") -> "Offline"
        value == "idle" -> "Ready"
        value == "conversation" -> "Listening"
        value == "browser_ptt" || value == "ptt" -> "PTT"
        value.contains("isolated_") || value.contains("engine") || value == "ready" || value == "online" -> "Online"
        else -> raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun StatusItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SoftBlue,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val unhealthy = value.lowercase() in setOf("offline", "error", "failed")
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (unhealthy) MaterialTheme.colorScheme.error else Success))
                Spacer(Modifier.width(5.dp))
                Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ConversationControls(viewModel: AppViewModel, activity: Activity) {
    val state by viewModel.ui.collectAsState()
    var micGranted by remember { mutableStateOf(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }
    val controllerReady = state.connected && state.session != null

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ConversationAction(
                    Icons.Outlined.PlayArrow,
                    if (state.conversationActive) "Mode" else "Start",
                    if (state.conversationActive) "Listening" else "Conversation",
                    MaterialTheme.colorScheme.primary,
                    enabled = controllerReady && !state.conversationActive && !state.busy,
                    onClick = viewModel::startConversation,
                )

                val pttModifier = when {
                    !controllerReady -> Modifier.clickable { viewModel.reportError("VerbaNode is not connected") }
                    !micGranted -> Modifier.clickable { micPermission.launch(Manifest.permission.RECORD_AUDIO) }
                    else -> Modifier.pointerInput(controllerReady, micGranted) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            viewModel.startPtt()
                            val up = waitForUpOrCancellation()
                            if (up != null) viewModel.stopPtt() else viewModel.cancelPtt()
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = pttModifier.padding(horizontal = 6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (state.recording) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        shadowElevation = if (controllerReady) 7.dp else 0.dp,
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            null,
                            tint = Color.White.copy(alpha = if (controllerReady) 1f else 0.55f),
                            modifier = Modifier.padding(20.dp).size(34.dp),
                        )
                    }
                    Text(
                        if (state.recording) "Release to Send" else "Hold to Talk",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                    Text(
                        if (controllerReady) "Push to speak anytime" else "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ConversationAction(
                    Icons.Outlined.Stop,
                    "Stop",
                    "Conversation",
                    MaterialTheme.colorScheme.error,
                    enabled = controllerReady && (state.conversationActive || state.recording || state.mode in setOf("browser_ptt", "ptt")) && !state.busy,
                    onClick = viewModel::stopConversation,
                )
            }
            Text(
                when {
                    state.recording -> "PTT recording from this phone"
                    state.conversationActive -> "Continuous conversation mode is active on the Windows host"
                    else -> "Start enables continuous host listening. Hold to Talk works independently."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun ConversationAction(
    icon: ImageVector,
    line1: String,
    line2: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.38f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
    ) {
        Surface(shape = CircleShape, color = tint.copy(alpha = if (enabled) 0.10f else 0.05f)) {
            Icon(icon, null, tint = tint.copy(alpha = alpha), modifier = Modifier.padding(13.dp).size(26.dp))
        }
        Text(line1, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), modifier = Modifier.padding(top = 7.dp))
        Text(line2, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    }
}

@Composable
internal fun ChatScreen(viewModel: AppViewModel, activity: Activity) {
    val state by viewModel.ui.collectAsState()
    var text by remember { mutableStateOf("") }
    ManagementScaffold(viewModel, "Chat", AppScreen.CHAT) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp)) {
            Feedback(viewModel)
            Card(modifier = Modifier.weight(1f).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                    if (state.messages.isEmpty()) item {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painterResource(R.drawable.verbanode_logo), "VerbaNode", Modifier.size(62.dp).clip(RoundedCornerShape(16.dp)))
                            Text("Start a conversation", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                            Text("Use text or Push-to-Talk with the active agent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(state.messages, key = { it.id }) { ChatBubble(it) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = SoftBlue) {
                    Column(Modifier.padding(10.dp)) { Text("Agent", style = MaterialTheme.typography.labelSmall); Text(state.activeAgent?.name ?: "None", fontWeight = FontWeight.SemiBold) }
                }
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = SoftBlue) {
                    Column(Modifier.padding(10.dp)) { Text("Mode", style = MaterialTheme.typography.labelSmall); Text(state.mode.ifBlank { "idle" }, fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(8.dp))
            ConversationControls(viewModel, activity)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(if (state.connected) "Type your message…" else "Reconnect to send messages") },
                    modifier = Modifier.weight(1f),
                    enabled = state.connected,
                    maxLines = 3,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { val value = text; text = ""; viewModel.sendMessage(value) },
                    enabled = state.connected && text.isNotBlank(),
                ) { Icon(Icons.Outlined.Send, "Send", tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val user = message.role == "user"
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        if (!user) {
            Image(painterResource(R.drawable.verbanode_logo), "VerbaNode", Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)))
            Spacer(Modifier.width(7.dp))
        }
        Card(
            modifier = Modifier.fillMaxWidth(0.82f),
            colors = CardDefaults.cardColors(containerColor = if (user) MaterialTheme.colorScheme.primaryContainer else SoftBlue),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(if (user) "You" else "VerbaNode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(message.content, modifier = Modifier.padding(top = 3.dp))
                message.createdAt?.let { Text(it.take(19).replace('T', ' '), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp)) }
            }
        }
    }
}

@Composable
internal fun MoreScreen(viewModel: AppViewModel) {
    ManagementScaffold(viewModel, "More / Management", AppScreen.MORE) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Feedback(viewModel) }
            item { MoreRow(Feature(Icons.Outlined.Extension, "Plugins", "Extend capabilities", viewModel::openPlugins), Feature(Icons.Outlined.Devices, "Devices", "Trusted controllers", viewModel::openDevices)) }
            item { MoreRow(Feature(Icons.Outlined.MonitorHeart, "Diagnostics", "System health", viewModel::openDiagnostics), Feature(Icons.Outlined.CloudUpload, "Backup & Restore", "Protect your data", viewModel::openData)) }
            item { MoreRow(Feature(Icons.Outlined.Settings, "Settings", "Conversation & runtime", viewModel::openSettings), Feature(Icons.Outlined.Tune, "AI & Models", "Models and engines", viewModel::openSettings)) }
            item { MoreRow(Feature(Icons.Outlined.GraphicEq, "Audio", "Input and output", viewModel::openSettings), Feature(Icons.Outlined.Security, "Security", "Trusted devices", viewModel::openDevices)) }
            item { MoreRow(Feature(Icons.Outlined.Person, "Agents", "Manage AI agents", viewModel::openAgents), Feature(Icons.Outlined.Info, "Information", "Agent knowledge", viewModel::openInformation)) }
            item { MoreRow(Feature(Icons.Outlined.Description, "Scripts & Queue", "TTS scripts", viewModel::openScripts), Feature(Icons.Outlined.Storage, "About / Status", "Core and protocol info", viewModel::openStatus)) }
            item { FeatureCard(Feature(Icons.Outlined.Logout, "Switch Server", "Return to connections", viewModel::goServers), Modifier.fillMaxWidth()) }
            item { OutlinedButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) { Text("Logout controller session") } }
        }
    }
}

@Composable
private fun MoreRow(first: Feature, second: Feature) = FeatureRow(first, second)
