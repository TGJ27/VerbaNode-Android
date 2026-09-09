package com.verbanode.mobile.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.verbanode.mobile.AppScreen
import com.verbanode.mobile.AppViewModel
import com.verbanode.mobile.BuildConfig
import com.verbanode.mobile.MainActivity
import com.verbanode.mobile.R
import com.verbanode.mobile.network.ChatMessage
import com.verbanode.mobile.pairing.scanVerbaNodeQr
import com.verbanode.mobile.storage.ServerProfile
import org.json.JSONObject

@Composable
fun VerbaNodeApp(viewModel: AppViewModel, activity: MainActivity) {
    val state by viewModel.ui.collectAsState()
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (state.screen) {
            AppScreen.SERVERS -> ServerScreen(viewModel, activity)
            AppScreen.TRUST -> TrustScreen(viewModel)
            AppScreen.LOGIN -> LoginScreen(viewModel, activity)
            AppScreen.HOME -> DashboardScreen(viewModel)
            AppScreen.CHAT -> ChatScreen(viewModel)
            AppScreen.AGENTS -> AgentsScreen(viewModel, activity)
            AppScreen.MORE -> MoreScreen(viewModel)
            AppScreen.KNOWLEDGE -> KnowledgeScreen(viewModel, activity)
            AppScreen.SCRIPTS -> ScriptsScreen(viewModel)
            AppScreen.AUDIO -> AudioLibraryScreen(viewModel, activity)
            AppScreen.TYPE_TO_TALK -> TypeToTalkScreen(viewModel)
            AppScreen.PUSH_TO_TALK -> PushToTalkScreen(viewModel, activity)
            AppScreen.PLUGINS -> PluginsScreen(viewModel)
            AppScreen.SETTINGS -> SettingsScreen(viewModel)
            AppScreen.DEVICES -> DevicesScreen(viewModel)
            AppScreen.DIAGNOSTICS -> DiagnosticsScreen(viewModel, activity)
            AppScreen.DATA -> DataScreen(viewModel, activity)
            AppScreen.STATUS -> StatusScreen(viewModel)
        }
        if (state.busy) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
    }
}

@Composable
internal fun Feedback(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    val text = state.error ?: state.notice ?: return
    val isError = state.error != null
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (isError) "Action failed" else "VerbaNode",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            }
            TextButton(onClick = viewModel::clearMessage) { Text("Dismiss") }
        }
    }
}

@Composable
private fun BrandHeader(subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.verbanode_logo),
            contentDescription = "VerbaNode",
            modifier = Modifier.size(52.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("VerbaNode", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface) {
            Text(
                "v${BuildConfig.VERSION_NAME}",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun Pill(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ServerScreen(viewModel: AppViewModel, activity: MainActivity) {
    val state by viewModel.ui.collectAsState()
    var address by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ConnectionEntryMode.SAVED) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Feedback(viewModel) }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    "Connect to VerbaNode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    "Find and connect to your server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        item { ConnectionModeTabs(selected = mode, onSelected = { mode = it }) }

        when (mode) {
            ConnectionEntryMode.SAVED -> {
                if (state.profiles.isEmpty()) {
                    item {
                        MockInfoPanel(
                            title = "No saved servers yet",
                            text = "Scan this Wi-Fi to find VerbaNode. Trusted servers will appear here after pairing.",
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column {
                                state.profiles.forEachIndexed { index, profile ->
                                    SavedServerRow(
                                        profile = profile,
                                        emphasized = index == 0,
                                        onConnect = { viewModel.connectProfile(profile) },
                                        onRemove = { viewModel.removeProfile(profile) },
                                    )
                                    if (index != state.profiles.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            ConnectionEntryMode.SCAN -> {
                item {
                    Button(
                        onClick = activity::ensureLocalNetworkAndDiscover,
                        enabled = !state.discoveryActive,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (state.discoveryActive) "Scanning this Wi-Fi…" else "Scan this Wi-Fi") }
                }
                if (state.discoveryActive) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(state.discoveryStage.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                state.discoveryWarning?.takeIf { it.isNotBlank() }?.let { warning ->
                    item { Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }
                }
                if (state.discovered.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column {
                                state.discovered.forEachIndexed { index, server ->
                                    DiscoveredServerRow(
                                        name = server.serviceName,
                                        address = server.baseUrl,
                                        emphasized = index == 0,
                                        onConnect = { viewModel.selectDiscovered(server) },
                                    )
                                    if (index != state.discovered.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                } else if (!state.discoveryActive) {
                    item { MockInfoPanel("Ready to scan", "Search the current Wi-Fi for a reachable VerbaNode server.") }
                }
            }

            ConnectionEntryMode.MANUAL -> {
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Server address") },
                        placeholder = { Text("e.g. 192.168.1.50") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                item {
                    Button(
                        onClick = { viewModel.probeServer(address) },
                        enabled = address.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Connect") }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("mDNS + UDP discovery", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("HTTPS fallback if needed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedServerRow(
    profile: ServerProfile,
    emphasized: Boolean,
    onConnect: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(profile.baseUrl.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (emphasized) {
            Button(onClick = onConnect, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text("Connect") }
        } else {
            OutlinedButton(onClick = onConnect, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text("Connect") }
        }
        TextButton(onClick = onRemove, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp)) { Text("×") }
    }
}

@Composable
private fun DiscoveredServerRow(name: String, address: String, emphasized: Boolean, onConnect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(address.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (emphasized) Button(onClick = onConnect) { Text("Connect") }
        else OutlinedButton(onClick = onConnect) { Text("Connect") }
    }
}

@Composable
private fun MockInfoPanel(title: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun ConnectionModeTabs(selected: ConnectionEntryMode, onSelected: (ConnectionEntryMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ConnectionEntryMode.entries.forEachIndexed { index, mode ->
            val active = mode == selected
            Column(
                modifier = Modifier.weight(1f).clickable { onSelected(mode) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    Phase1UiSpec.connectionTabs[index],
                    modifier = Modifier.padding(vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(
                    thickness = if (active) 2.dp else 1.dp,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun ConnectionSectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ConnectEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(28.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: ServerProfile, viewModel: AppViewModel) {
    SavedServerRow(
        profile = profile,
        emphasized = true,
        onConnect = { viewModel.connectProfile(profile) },
        onRemove = { viewModel.removeProfile(profile) },
    )
}

@Composable
private fun TrustScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    val probe = state.trustCandidate
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Feedback(viewModel) }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(34.dp))
                }
                Text("Verify this VerbaNode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                Text("Confirm the local server identity before pairing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(probe?.clientInfo?.instanceName ?: "VerbaNode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(probe?.baseUrl.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Pill("CORE ${probe?.clientInfo?.serverVersion ?: "?"}")
                        Pill("API ${probe?.clientInfo?.apiVersion ?: "?"}")
                    }
                    Text("TLS identity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                        Text(probe?.certificateSpkiSha256.orEmpty(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                    Button(onClick = viewModel::confirmTrust, enabled = probe != null, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) { Text("Trust & continue") }
                    TextButton(onClick = viewModel::goServers, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(viewModel: AppViewModel, activity: MainActivity) {
    val state by viewModel.ui.collectAsState()
    var pairingCode by remember { mutableStateOf("") }
    var controllerPin by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("code") }
    var useControllerPin by remember { mutableStateOf(false) }
    var trustDevice by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Feedback(viewModel) }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(34.dp))
                }
                Text("Pairing / Authentication", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                Text("Pair with your VerbaNode server", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth()) {
                Phase1UiSpec.pairingTabs.forEachIndexed { index, label ->
                    val key = if (index == 0) "code" else "qr"
                    val active = mode == key
                    Column(
                        modifier = Modifier.weight(1f).clickable { mode = key },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(thickness = if (active) 2.dp else 1.dp, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        if (mode == "code") {
            item {
                if (useControllerPin) {
                    OutlinedTextField(
                        value = controllerPin,
                        onValueChange = { controllerPin = it.take(32) },
                        label = { Text("Controller PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                } else {
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { pairingCode = it.filter(Char::isDigit).take(12) },
                        label = { Text("Enter pairing code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            if (useControllerPin) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = trustDevice, onCheckedChange = { trustDevice = it })
                        Text("Trust this phone after PIN login", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (useControllerPin) viewModel.loginWithPin(controllerPin, trustDevice)
                        else viewModel.pairWithShortCode(pairingCode)
                    },
                    enabled = if (useControllerPin) controllerPin.isNotBlank() else pairingCode.length >= 6,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (useControllerPin) "Login" else "Pair") }
            }
            item {
                TextButton(onClick = { useControllerPin = !useControllerPin }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (useControllerPin) "Use pairing code instead" else "Use controller PIN instead")
                }
            }
        } else {
            item {
                OutlinedButton(
                    onClick = { scanVerbaNodeQr(activity, activity::pairFromQrWithPermission, viewModel::reportError) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Scan QR code") }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color(0xFFE8F8F1),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.tertiary) {
                        Text("✓", color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Trusted Connection", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Text(
                            if (state.currentProfile?.paired == true) "This device is already paired and trusted with this server."
                            else "This device will be trusted with this server after pairing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { TextButton(onClick = viewModel::goServers, modifier = Modifier.fillMaxWidth()) { Text("Back to servers") } }
    }
}

@Composable
internal fun DashboardCard(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
            else Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.ifBlank { "unknown" }, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
        }
    }
}
