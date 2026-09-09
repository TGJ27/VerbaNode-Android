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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.verbanode.mobile.AppScreen
import com.verbanode.mobile.AppViewModel
import com.verbanode.mobile.R
import com.verbanode.mobile.network.ChatMessage

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
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (selected == AppScreen.HOME) {
                Column(
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "VerbaNode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = viewModel::openSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                    }
                    CompactServerStatus(viewModel)
                }
            } else {
                Column(
                    Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.verbanode_logo),
                            contentDescription = "VerbaNode logo",
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("VerbaNode", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::openSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                    }
                    if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, start = 2.dp))
                }
            }
        },
        bottomBar = {
            if (selected == AppScreen.HOME) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(true, viewModel::openHome, { Icon(Icons.Outlined.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(false, viewModel::openAgents, { Icon(Icons.Outlined.Person, null) }, label = { Text("Agents") })
                    NavigationBarItem(false, viewModel::openChat, { Icon(Icons.AutoMirrored.Outlined.Chat, null) }, label = { Text("Chat") })
                    NavigationBarItem(false, { viewModel.navigate(AppScreen.MORE) }, { Icon(Icons.Outlined.MoreHoriz, null) }, label = { Text("More") })
                }
            } else {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(selected == AppScreen.HOME, viewModel::openHome, { Icon(Icons.Outlined.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(selected == AppScreen.CHAT, viewModel::openChat, { Icon(Icons.AutoMirrored.Outlined.Chat, null) }, label = { Text("Chat") })
                    NavigationBarItem(selected == AppScreen.AGENTS, viewModel::openAgents, { Icon(Icons.Outlined.Person, null) }, label = { Text("Agents") })
                    NavigationBarItem(selected == AppScreen.KNOWLEDGE, viewModel::openKnowledge, { Icon(Icons.Outlined.Storage, null) }, label = { Text("Knowledge") })
                    NavigationBarItem(selected == AppScreen.MORE, { viewModel.navigate(AppScreen.MORE) }, { Icon(Icons.Outlined.MoreHoriz, null) }, label = { Text("More") })
                }
            }
        },
        content = content,
    )
}

@Composable
private fun CompactServerStatus(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = viewModel::goServers).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.connected) Success else MaterialTheme.colorScheme.error))
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (state.connected) "Connected" else state.connectionLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.connected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            )
            Text(
                buildString {
                    append(state.currentProfile?.name ?: "VerbaNode")
                    state.currentProfile?.baseUrl?.removePrefix("https://")?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun DashboardScreen(viewModel: AppViewModel) {
    val specs = Phase1UiSpec.homeCards
    ManagementScaffold(viewModel, "", AppScreen.HOME) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item { Feedback(viewModel) }
            item {
                FeatureRow(
                    Feature(Icons.Outlined.Person, specs[0].title, specs[0].subtitle, viewModel::openAgents),
                    Feature(Icons.Outlined.Storage, specs[1].title, specs[1].subtitle, viewModel::openKnowledge),
                )
            }
            item {
                FeatureRow(
                    Feature(Icons.AutoMirrored.Outlined.Chat, specs[2].title, specs[2].subtitle, viewModel::openChat),
                    Feature(Icons.AutoMirrored.Outlined.VolumeUp, specs[3].title, specs[3].subtitle, viewModel::openTypeToTalk),
                )
            }
            item {
                FeatureRow(
                    Feature(Icons.Outlined.Mic, specs[4].title, specs[4].subtitle, viewModel::openPushToTalk),
                    Feature(Icons.Outlined.Description, specs[5].title, specs[5].subtitle, viewModel::openScripts),
                )
            }
            item {
                FeatureRow(
                    Feature(Icons.Outlined.GraphicEq, specs[6].title, specs[6].subtitle, viewModel::openAudio),
                    Feature(Icons.Outlined.CloudUpload, specs[7].title, specs[7].subtitle, viewModel::openData),
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    FeatureCard(Feature(Icons.Outlined.MonitorHeart, specs[8].title, specs[8].subtitle, viewModel::openDiagnostics), Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
            }
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
        modifier = modifier.heightIn(min = 88.dp).clickable(onClick = feature.action),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Icon(feature.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(feature.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp))
            Text(
                feature.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ConversationControls(viewModel: AppViewModel, activity: Activity) {
    val state by viewModel.ui.collectAsState()
    var micGranted by remember { mutableStateOf(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }
    val controllerReady = state.connected && state.session != null

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text("Push to Talk", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                "Voice controls remain available here in Phase 1; the dedicated Push to Talk screen is polished in Phase 2.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = pttModifier.padding(horizontal = 5.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (state.recording) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        shadowElevation = if (controllerReady) 5.dp else 0.dp,
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            null,
                            tint = Color.White.copy(alpha = if (controllerReady) 1f else 0.55f),
                            modifier = Modifier.padding(13.dp).size(28.dp),
                        )
                    }
                    Text(
                        if (state.recording) "Release" else "Hold to Talk",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                ConversationAction(
                    Icons.Outlined.Stop,
                    if (state.recording) "Cancel" else "Stop audio",
                    MaterialTheme.colorScheme.error,
                    enabled = controllerReady && (state.recording || state.chatStatus in setOf("Speaking", "Preparing speech")) && !state.busy,
                    onClick = { if (state.recording) viewModel.cancelPtt() else viewModel.stopTts() },
                )
            }
        }
    }
}

@Composable
private fun ConversationAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.38f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(3.dp),
    ) {
        Surface(shape = CircleShape, color = tint.copy(alpha = if (enabled) 0.10f else 0.05f)) {
            Icon(icon, null, tint = tint.copy(alpha = alpha), modifier = Modifier.padding(10.dp).size(24.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
internal fun ChatScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    val listState = rememberLazyListState()
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size, state.chatAutoScroll) {
        if (state.chatAutoScroll && state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::openHome) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                    Text("Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Box {
                        IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Outlined.MoreVert, "Chat options") }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("New chat") }, onClick = { menuExpanded = false; viewModel.newConversation() })
                            DropdownMenuItem(text = { Text("Clear chat") }, onClick = { menuExpanded = false; viewModel.clearCurrentConversation() }, enabled = state.conversationId != null && state.messages.isNotEmpty())
                            DropdownMenuItem(text = { Text(if (state.chatAutoScroll) "Auto-scroll: On" else "Auto-scroll: Off") }, onClick = { viewModel.setChatAutoScroll(!state.chatAutoScroll); menuExpanded = false })
                            DropdownMenuItem(text = { Text("Push to Talk") }, onClick = { menuExpanded = false; viewModel.openPushToTalk() })
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            Feedback(viewModel)
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable(onClick = viewModel::openAgents),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(Icons.Outlined.Person, null, tint = Color.White, modifier = Modifier.padding(4.dp).size(12.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(state.activeAgent?.name ?: "No agent", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (state.conversationActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable(
                        enabled = state.connected && state.session != null && !state.busy,
                        onClick = { if (state.conversationActive) viewModel.stopConversation() else viewModel.startConversation() },
                    ),
                ) {
                    Text(
                        if (state.conversationActive) "∞  Convo Mode ON" else "∞  Convo Mode OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.conversationActive) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
            Text(
                if (state.conversationActive) "Windows host listening" else "Windows host listening is off",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
            val compactStatus = Phase3UiSpec.chatStatusLabel(state.connected, state.chatStatus)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape).background(
                        when (compactStatus) {
                            "Offline" -> MaterialTheme.colorScheme.error
                            "Ready" -> Success
                            "Speaking" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    compactStatus,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.messages.isEmpty()) item {
                    Text(
                        "Start a conversation with ${state.activeAgent?.name ?: "your agent"}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                items(state.messages, key = { it.id }) { ChatBubble(it) }
            }

            state.chatRetryText?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Message not sent. Draft restored.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::retryChatMessage) { Text("Retry") }
                    }
                }
            }


            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 5.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = state.chatDraft,
                        onValueChange = viewModel::updateChatDraft,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 10.dp),
                        enabled = state.connected && state.chatPendingText == null,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            Box {
                                if (state.chatDraft.isBlank()) {
                                    Text(
                                        if (state.connected) "Type a message…" else "Reconnect to send messages",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    Spacer(Modifier.width(5.dp))
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        IconButton(
                            onClick = viewModel::sendChatDraft,
                            enabled = state.connected && state.chatDraft.isNotBlank() && state.chatPendingText == null,
                        ) { Icon(Icons.AutoMirrored.Outlined.Send, "Send", tint = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val user = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.76f),
            color = if (user) MaterialTheme.colorScheme.primaryContainer else SoftBlue,
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                Text(message.content, style = MaterialTheme.typography.bodyMedium)
                message.createdAt?.let {
                    Text(
                        it.take(19).replace('T', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
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
            item { MoreRow(Feature(Icons.Outlined.Mic, "Type to Talk", "Queue direct TTS speech", viewModel::openTypeToTalk), Feature(Icons.Outlined.Settings, "Settings", "Conversation & runtime", viewModel::openSettings)) }
            item { MoreRow(Feature(Icons.Outlined.GraphicEq, "Audio", "Multi-format audio library", viewModel::openAudio), Feature(Icons.Outlined.Security, "Security", "Trusted devices", viewModel::openDevices)) }
            item { MoreRow(Feature(Icons.Outlined.Person, "Agents", "Manage AI agents", viewModel::openAgents), Feature(Icons.Outlined.Info, "Knowledge", "Hybrid RAG libraries", viewModel::openKnowledge)) }
            item { MoreRow(Feature(Icons.Outlined.Description, "Scripts & Queue", "TTS scripts", viewModel::openScripts), Feature(Icons.Outlined.Storage, "About / Status", "Core and protocol info", viewModel::openStatus)) }
            item { FeatureCard(Feature(Icons.AutoMirrored.Outlined.Logout, "Switch Server", "Return to connections", viewModel::goServers), Modifier.fillMaxWidth()) }
            item { OutlinedButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) { Text("Logout controller session") } }
        }
    }
}

@Composable
private fun MoreRow(first: Feature, second: Feature) = FeatureRow(first, second)
