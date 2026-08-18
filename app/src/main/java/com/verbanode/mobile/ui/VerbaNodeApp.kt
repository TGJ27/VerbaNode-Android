package com.verbanode.mobile.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
            .safeDrawingPadding()
    ) {
        when (state.screen) {
            AppScreen.SERVERS -> ServerScreen(viewModel, activity)
            AppScreen.TRUST -> TrustScreen(viewModel)
            AppScreen.LOGIN -> LoginScreen(viewModel)
            AppScreen.HOME -> DashboardScreen(viewModel)
            AppScreen.CHAT -> ChatScreen(viewModel, activity)
            AppScreen.AGENTS -> AgentsScreen(viewModel, activity)
            AppScreen.MORE -> MoreScreen(viewModel)
            AppScreen.INFORMATION -> InformationScreen(viewModel)
            AppScreen.SCRIPTS -> ScriptsScreen(viewModel)
            AppScreen.AUDIO -> AudioLibraryScreen(viewModel, activity)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.verbanode_logo),
                contentDescription = "VerbaNode",
                modifier = Modifier.size(58.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("VerbaNode", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Pill("LAN ONLY")
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BrandHeader("Android management console · v${BuildConfig.VERSION_NAME}") }
        item { Feedback(viewModel) }
        item {
            DashboardCard(
                title = "Connect to VerbaNode",
                subtitle = "Choose a saved server, run one LAN scan, scan a pairing QR, or connect manually.",
            ) {
                Button(
                    onClick = activity::ensureLocalNetworkAndDiscover,
                    enabled = !state.discoveryActive,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.discoveryActive) "Scanning this Wi-Fi…" else "Scan this Wi-Fi once") }
                OutlinedButton(
                    onClick = { scanVerbaNodeQr(activity, activity::pairFromQrWithPermission, viewModel::reportError) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Scan pairing QR") }
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Pill(if (state.discoveryActive) "SCANNING" else "ONE-SHOT")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.discoveryActive) "Discovery stops automatically after 6.5 seconds."
                        else "Results stay available until you choose to scan again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (state.discovered.isNotEmpty()) {
            item { SectionTitle("Found on this network") }
            items(state.discovered, key = { it.baseUrl }) { server ->
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Text(
                            server.serviceName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            server.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Pill("Core ${server.version ?: "?"}")
                            Pill("API ${server.apiVersion ?: "?"}")
                        }
                        Button(
                            onClick = { viewModel.selectDiscovered(server) },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        ) { Text("Connect") }
                    }
                }
            }
        }
        if (state.profiles.isNotEmpty()) {
            item { SectionTitle("Saved VerbaNodes") }
            items(state.profiles, key = { it.id }) { profile -> ProfileCard(profile, viewModel) }
        }
        item {
            DashboardCard(title = "Manual connection", subtitle = "Use the HTTPS host/IP and port shown by VerbaNode on Windows.") {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Server address") },
                    placeholder = { Text("192.168.1.20:8002") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.probeServer(address) },
                    enabled = address.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) { Text("Connect") }
            }
        }
        item {
            Text(
                "Local network only. This app does not use cloud relay or Internet remote control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }
    }
}

@Composable
private fun ProfileCard(profile: ServerProfile, viewModel: AppViewModel) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                profile.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                profile.baseUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Pill(if (profile.paired) "TRUSTED" else "PIN REQUIRED")
                Pill("CORE ${profile.lastServerVersion ?: "?"}")
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.connectProfile(profile) }, modifier = Modifier.weight(1f)) { Text("Open") }
                OutlinedButton(onClick = { viewModel.removeProfile(profile) }, modifier = Modifier.weight(1f)) { Text("Remove") }
            }
        }
    }
}

@Composable
private fun TrustScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    val probe = state.trustCandidate
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandHeader("Verify local server")
        Feedback(viewModel)
        DashboardCard(title = "Trust this VerbaNode?", subtitle = "The certificate identity is saved locally so future HTTPS/WSS connections must match this server.") {
            Text(probe?.clientInfo?.instanceName ?: "VerbaNode", fontWeight = FontWeight.Bold)
            Text(probe?.baseUrl.orEmpty(), style = MaterialTheme.typography.bodySmall)
            Text("Core ${probe?.clientInfo?.serverVersion ?: "?"} · API ${probe?.clientInfo?.apiVersion ?: "?"}", style = MaterialTheme.typography.bodySmall)
            Text("SPKI SHA-256", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp))
            Text(probe?.certificateSpkiSha256.orEmpty(), style = MaterialTheme.typography.bodySmall)
            Button(onClick = viewModel::confirmTrust, enabled = probe != null, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("Trust & continue") }
            OutlinedButton(onClick = viewModel::goServers, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Cancel") }
        }
    }
}

@Composable
private fun LoginScreen(viewModel: AppViewModel) {
    val state by viewModel.ui.collectAsState()
    var pin by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var trustDevice by remember { mutableStateOf(true) }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BrandHeader(state.currentProfile?.name ?: "Controller login") }
        item { Feedback(viewModel) }
        item {
            DashboardCard(title = "Controller PIN", subtitle = "Authenticate to manage this VerbaNode. You can trust this phone so later logins are automatic.") {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.take(32) },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = trustDevice, onCheckedChange = { trustDevice = it })
                    Text("Trust this phone after PIN login")
                }
                Button(onClick = { viewModel.loginWithPin(pin, trustDevice) }, enabled = pin.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Login") }
            }
        }
        item {
            DashboardCard(title = "Pair with code", subtitle = "Web dashboard → Settings → Devices → Pair new device.") {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(12) },
                    label = { Text("Pairing code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { viewModel.pairWithShortCode(code) }, enabled = code.length >= 6, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Pair this phone") }
            }
        }
        item { OutlinedButton(onClick = viewModel::goServers, modifier = Modifier.fillMaxWidth()) { Text("Back to servers") } }
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
