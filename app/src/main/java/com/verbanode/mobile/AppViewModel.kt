package com.verbanode.mobile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verbanode.mobile.audio.PttRecorder
import com.verbanode.mobile.discovery.DiscoveredServer
import com.verbanode.mobile.discovery.LanDiscovery
import com.verbanode.mobile.network.Agent
import com.verbanode.mobile.network.ApiException
import com.verbanode.mobile.network.AuthSession
import com.verbanode.mobile.network.ApiSessionContext
import com.verbanode.mobile.network.BootstrapData
import com.verbanode.mobile.network.ChatMessage
import com.verbanode.mobile.network.ClientInfo
import com.verbanode.mobile.network.ConnectionState
import com.verbanode.mobile.network.ManagementRepository
import com.verbanode.mobile.network.ProbeResult
import com.verbanode.mobile.network.requireAndroidCompatibility
import com.verbanode.mobile.network.TlsTrust
import com.verbanode.mobile.network.TrustedDevice
import com.verbanode.mobile.network.VerbaNodeApi
import com.verbanode.mobile.network.VerbaNodeWebSocket
import com.verbanode.mobile.pairing.parsePairingLink
import com.verbanode.mobile.storage.ProfileStore
import com.verbanode.mobile.storage.ServerProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID



class AppViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val DISCOVERY_WINDOW_MS = 6500L
        private const val PTT_START_TIMEOUT_MS = 9000L
        private const val PTT_START_GATE_TIMEOUT_MS = 10_000L
    }

    private val store = ProfileStore(application)
    private val recorder = PttRecorder()
    private val _ui = MutableStateFlow(MobileUiState())
    val ui: StateFlow<MobileUiState> = _ui.asStateFlow()
    private val management = ManagementRepository { requireApiSession() }

    private var api: VerbaNodeApi? = null
    private var webSocket: VerbaNodeWebSocket? = null
    private var discovery: LanDiscovery? = null
    private var discoveryStopJob: Job? = null
    private var pttStart: CompletableDeferred<Boolean>? = null
    private var pttStartJob: Job? = null

    init {
        reloadProfiles()
    }

    private fun beginOperation(label: String, clearFeedback: Boolean): String {
        val id = "$label:${UUID.randomUUID()}"
        _ui.update { state ->
            state.copy(
                activeOperations = state.activeOperations + id,
                error = if (clearFeedback) null else state.error,
                notice = if (clearFeedback) null else state.notice,
            )
        }
        return id
    }

    private fun endOperation(id: String) {
        _ui.update { state -> state.copy(activeOperations = state.activeOperations - id) }
    }

    private fun runBusy(label: String = "request", block: suspend () -> Unit) {
        viewModelScope.launch {
            val operation = beginOperation(label, clearFeedback = true)
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _ui.update { it.copy(error = friendlyError(error)) }
            } finally {
                endOperation(operation)
            }
        }
    }

    fun clearMessage() = _ui.update { it.copy(error = null, notice = null) }
    fun reportError(message: String) = _ui.update { it.copy(error = message, notice = null) }
    fun navigate(screen: AppScreen) = _ui.update { it.copy(screen = screen, error = null, notice = null) }

    fun reloadProfiles() = viewModelScope.launch {
        val profiles = store.profiles()
        _ui.update { it.copy(profiles = profiles) }
    }

    fun startDiscovery() {
        if (discovery != null) return
        discoveryStopJob?.cancel()
        val service = LanDiscovery(
            getApplication<Application>(),
            onUpdate = { values -> viewModelScope.launch { _ui.update { it.copy(discovered = values) } } },
            onError = { message ->
                viewModelScope.launch {
                    discoveryStopJob?.cancel()
                    discoveryStopJob = null
                    discovery = null
                    _ui.update { it.copy(error = message, discoveryActive = false) }
                }
            },
        )
        discovery = service
        _ui.update { it.copy(discoveryActive = true, discovered = emptyList(), error = null, notice = null) }
        service.start()
        discoveryStopJob = viewModelScope.launch {
            delay(DISCOVERY_WINDOW_MS)
            stopDiscovery(clearResults = false)
            if (_ui.value.discovered.isEmpty()) {
                _ui.update { it.copy(notice = "No VerbaNode found. Scan again or use manual connection.") }
            }
        }
    }

    fun stopDiscovery(clearResults: Boolean = false) {
        discoveryStopJob?.cancel()
        discoveryStopJob = null
        discovery?.stop(clearResults = clearResults)
        discovery = null
        _ui.update {
            it.copy(
                discoveryActive = false,
                discovered = if (clearResults) emptyList() else it.discovered,
            )
        }
    }

    fun selectDiscovered(server: DiscoveredServer) {
        stopDiscovery(clearResults = false)
        runBusy {
            val profiles = store.profiles()
            val existing = profiles.firstOrNull { profile ->
                (!server.instanceId.isNullOrBlank() && profile.instanceId == server.instanceId) ||
                    (server.spkiSha256 != null && profile.spkiSha256 == server.spkiSha256)
            }
            if (existing != null) {
                val updated = existing.copy(baseUrl = server.baseUrl)
                store.upsert(updated)
                val advertisedSpki = server.spkiSha256?.lowercase()
                if (!advertisedSpki.isNullOrBlank() && advertisedSpki != updated.spkiSha256.lowercase()) {
                    // Same persistent Core instance, refreshed TLS identity. Re-trust the key but
                    // preserve the stored trusted-device credential; an app/server version change
                    // must never create a new device profile.
                    probeInternal(server.baseUrl, advertisedSpki, updated)
                } else {
                    connectProfileInternal(updated)
                }
            } else {
                probeInternal(server.baseUrl, server.spkiSha256)
            }
        }
    }

    fun probeServer(address: String) = runBusy { probeInternal(address) }

    private suspend fun probeInternal(
        address: String,
        expectedSpkiSha256: String? = null,
        existingProfile: ServerProfile? = null,
    ) {
        val result = withContext(Dispatchers.IO) { TlsTrust.probe(address, expectedSpkiSha256) }
        _ui.update {
            it.copy(
                screen = AppScreen.TRUST,
                trustCandidate = result,
                clientInfo = result.clientInfo,
                currentProfile = existingProfile,
            )
        }
    }

    fun confirmTrust() = runBusy {
        val candidate = _ui.value.trustCandidate ?: error("No server is waiting for trust confirmation")
        val remembered = _ui.value.currentProfile
        val existing = remembered?.takeIf {
            (!candidate.clientInfo.instanceId.isNullOrBlank() && it.instanceId == candidate.clientInfo.instanceId) ||
                it.spkiSha256 == candidate.certificateSpkiSha256
        } ?: store.profiles().firstOrNull {
            (!candidate.clientInfo.instanceId.isNullOrBlank() && it.instanceId == candidate.clientInfo.instanceId) ||
                it.spkiSha256 == candidate.certificateSpkiSha256
        }
        val profile = ServerProfile(
            id = existing?.id ?: candidate.clientInfo.instanceId ?: UUID.randomUUID().toString(),
            name = candidate.clientInfo.instanceName ?: "VerbaNode",
            baseUrl = candidate.baseUrl,
            spkiSha256 = candidate.certificateSpkiSha256,
            instanceId = candidate.clientInfo.instanceId,
            deviceId = existing?.deviceId,
            encryptedDeviceToken = existing?.encryptedDeviceToken,
            lastServerVersion = candidate.clientInfo.serverVersion,
        )
        store.upsert(profile)
        val profiles = store.profiles()
        api = VerbaNodeApi(profile.baseUrl, profile.spkiSha256)
        _ui.update {
            it.copy(
                screen = AppScreen.LOGIN,
                currentProfile = profile,
                trustCandidate = null,
                profiles = profiles,
            )
        }
        if (profile.paired) connectProfileInternal(profile)
    }

    fun connectProfile(profile: ServerProfile) = runBusy { connectProfileInternal(profile) }

    private suspend fun connectProfileInternal(profile: ServerProfile) {
        val localApi = VerbaNodeApi(profile.baseUrl, profile.spkiSha256)
        val info = withContext(Dispatchers.IO) { localApi.clientInfo() }
        info.requireAndroidCompatibility()
        api = localApi
        val refreshed = profile.copy(
            name = info.instanceName ?: profile.name,
            instanceId = info.instanceId ?: profile.instanceId,
            lastServerVersion = info.serverVersion,
        )
        store.upsert(refreshed)
        _ui.update { it.copy(currentProfile = refreshed, clientInfo = info, screen = AppScreen.LOGIN) }
        if (refreshed.paired) {
            val token = refreshed.encryptedDeviceToken?.let(store.secretBox::decrypt)
            if (!token.isNullOrBlank() && !refreshed.deviceId.isNullOrBlank()) {
                try {
                    val session = withContext(Dispatchers.IO) {
                        localApi.deviceLogin(refreshed.deviceId, token, android.os.Build.MODEL)
                    }
                    completeSession(session)
                } catch (error: ApiException) {
                    if (error.status != 401) throw error
                    _ui.update { it.copy(notice = "This device credential was revoked. Use PIN or pair again.") }
                }
            }
        }
    }

    fun loginWithPin(pin: String, trustThisDevice: Boolean) = runBusy {
        val localApi = api ?: error("Connect to VerbaNode first")
        val session = withContext(Dispatchers.IO) { localApi.pinLogin(pin, android.os.Build.MODEL) }
        if (trustThisDevice) {
            val pairing = withContext(Dispatchers.IO) { localApi.startPairing(session.token) }
            val claim = withContext(Dispatchers.IO) {
                localApi.claimPairing(
                    pairingId = pairing.getString("pairing_id"),
                    secret = pairing.getString("pairing_uri").let { android.net.Uri.parse(it).getQueryParameter("secret") },
                    shortCode = null,
                    deviceName = android.os.Build.MODEL,
                )
            }
            saveTrustedCredential(claim.deviceId, claim.deviceToken, claim.deviceName, claim.serverUrl, claim.spkiSha256)
            val trustedSession = withContext(Dispatchers.IO) {
                localApi.deviceLogin(claim.deviceId, claim.deviceToken, claim.deviceName)
            }
            completeSession(trustedSession)
        } else {
            completeSession(session)
        }
    }

    fun pairWithShortCode(code: String) = runBusy {
        val localApi = api ?: error("Trust the VerbaNode server first")
        val claim = withContext(Dispatchers.IO) {
            localApi.claimPairing(null, null, code.filter(Char::isDigit), android.os.Build.MODEL)
        }
        val profile = saveTrustedCredential(claim.deviceId, claim.deviceToken, claim.deviceName, claim.serverUrl, claim.spkiSha256)
        api = VerbaNodeApi(profile.baseUrl, profile.spkiSha256)
        val session = withContext(Dispatchers.IO) { api!!.deviceLogin(claim.deviceId, claim.deviceToken, claim.deviceName) }
        completeSession(session)
    }

    fun pairFromQr(raw: String) = runBusy {
        val link = parsePairingLink(raw)
        val localApi = VerbaNodeApi(link.serverUrl, link.spkiSha256)
        val info = withContext(Dispatchers.IO) { localApi.clientInfo() }
        info.requireAndroidCompatibility()
        if (info.certificateSpkiSha256 != link.spkiSha256) error("The QR pairing identity does not match the server")
        val claim = withContext(Dispatchers.IO) {
            localApi.claimPairing(link.pairingId, link.secret, null, android.os.Build.MODEL)
        }
        val profile = saveTrustedCredential(claim.deviceId, claim.deviceToken, claim.deviceName, claim.serverUrl, link.spkiSha256, info)
        api = VerbaNodeApi(profile.baseUrl, profile.spkiSha256)
        val session = withContext(Dispatchers.IO) { api!!.deviceLogin(claim.deviceId, claim.deviceToken, claim.deviceName) }
        completeSession(session)
    }

    private suspend fun saveTrustedCredential(
        deviceId: String,
        deviceToken: String,
        name: String,
        serverUrl: String,
        spki: String,
        info: ClientInfo? = _ui.value.clientInfo,
    ): ServerProfile {
        val current = _ui.value.currentProfile
        val encrypted = store.secretBox.encrypt(deviceToken)
        val profile = ServerProfile(
            id = current?.id ?: info?.instanceId ?: UUID.randomUUID().toString(),
            name = info?.instanceName ?: current?.name ?: name,
            baseUrl = serverUrl.ifBlank { current?.baseUrl.orEmpty() },
            spkiSha256 = spki.ifBlank { current?.spkiSha256.orEmpty() },
            instanceId = info?.instanceId ?: current?.instanceId,
            deviceId = deviceId,
            encryptedDeviceToken = encrypted,
            lastServerVersion = info?.serverVersion ?: current?.lastServerVersion,
        )
        store.upsert(profile)
        val profiles = store.profiles()
        _ui.update { it.copy(currentProfile = profile, profiles = profiles) }
        return profile
    }

    private suspend fun completeSession(session: AuthSession) {
        webSocket?.close()
        _ui.update {
            it.copy(
                session = session,
                screen = AppScreen.HOME,
                connectionState = ConnectionState.CONNECTING,
                connectionLabel = "Connecting",
                error = null,
            )
        }
        loadBootstrapInternal()
        loadDashboardInternal()
        connectWebSocket(session)
    }

    private suspend fun loadBootstrapInternal() {
        val localApi = api ?: return
        val token = _ui.value.session?.token ?: return
        val bootstrap = withContext(Dispatchers.IO) { localApi.bootstrap(token) }
        applyBootstrap(bootstrap)
    }

    private fun applyBootstrap(data: BootstrapData) {
        _ui.update {
            it.copy(
                agents = data.agents,
                activeAgent = data.activeAgent,
                conversationId = data.conversationId,
                messages = data.messages,
                mode = data.mode,
                conversationActive = data.mode == "conversation",
                recording = if (data.mode == "browser_ptt") it.recording else false,
                chatStatus = when {
                    !data.sttMode.isNullOrBlank() && data.sttMode != "idle" -> pipelineStatusLabel(data.sttMode, data.mode)
                    !data.aiMode.isNullOrBlank() && data.aiMode != "idle" -> pipelineStatusLabel(data.aiMode, data.mode)
                    !data.ttsMode.isNullOrBlank() && data.ttsMode != "idle" -> pipelineStatusLabel(data.ttsMode, data.mode)
                    else -> modeStatusLabel(data.mode)
                },
            )
        }
    }

    private fun connectWebSocket(session: AuthSession) {
        val localApi = api ?: return
        webSocket = VerbaNodeWebSocket(
            localApi,
            session.token,
            onEvent = { type, data ->
                viewModelScope.launch {
                    when (type) {
                        "message_added", "conversation_changed", "conversation_cleared" -> refreshConversationInternal()
                        "assistant_start", "assistant_token" -> _ui.update { it.copy(chatStatus = "Generating") }
                        "assistant_complete" -> {
                            refreshConversationInternal()
                            _ui.update { it.copy(chatStatus = if (it.mode == "conversation") "Listening" else "Ready") }
                        }
                        "stt_started" -> _ui.update { it.copy(chatStatus = "Transcribing") }
                        "listening" -> if (data?.optBoolean("active", false) == true) _ui.update { it.copy(chatStatus = "Listening") }
                        "tts_started" -> _ui.update { it.copy(chatStatus = "Preparing speech") }
                        "tts_chunk_generating" -> _ui.update { it.copy(chatStatus = "Preparing speech") }
                        "tts_chunk" -> _ui.update { it.copy(chatStatus = "Speaking") }
                        "tts_stopped" -> _ui.update { it.copy(chatStatus = if (it.mode == "conversation") "Listening" else "Ready") }
                        "pipeline_state" -> data?.optString("state")?.takeIf { it.isNotBlank() }?.let { stage ->
                            _ui.update { it.copy(pipelineStatus = data, chatStatus = pipelineStatusLabel(stage, it.mode)) }
                        }
                        "agents_changed", "agent_changed" -> { loadBootstrapInternal(); if (_ui.value.screen == AppScreen.AGENTS) loadAgentsManagementInternal() }
                        "plugins_changed" -> if (_ui.value.screen == AppScreen.PLUGINS) loadPluginsInternal()
                        "scripts_changed", "queue_changed", "queue_state", "script_defaults_changed" -> if (_ui.value.screen == AppScreen.SCRIPTS) loadScriptsInternal()
                        "type_to_talk_queue" -> if (_ui.value.screen == AppScreen.TYPE_TO_TALK) loadTypeToTalkInternal()
                        "audio_library_changed", "audio_library_state" -> if (_ui.value.screen == AppScreen.AUDIO) loadAudioLibraryInternal()
                        "models_changed", "model_pull" -> {
                            if (_ui.value.screen == AppScreen.SETTINGS) loadSettingsInternal()
                            if (_ui.value.screen == AppScreen.AGENTS || _ui.value.screen == AppScreen.SCRIPTS) loadConfigurationOptionsInternal()
                        }
                        "reload_required" -> _ui.update { it.copy(notice = "VerbaNode restored data. Restart Core before continuing management changes.") }
                        "mode_changed" -> data?.optString("mode")?.let { mode ->
                            _ui.update {
                                it.copy(
                                    mode = mode,
                                    conversationActive = mode == "conversation",
                                    recording = if (mode == "browser_ptt") it.recording else false,
                                    chatStatus = modeStatusLabel(mode),
                                )
                            }
                        }
                        "control_revoked" -> sessionLost("Control moved to another client")
                    }
                }
            },
            onState = { connectionState, label ->
                viewModelScope.launch {
                    _ui.update { state ->
                        state.copy(
                            connectionState = connectionState,
                            connectionLabel = label,
                            chatStatus = when (connectionState) {
                                ConnectionState.CONNECTED -> if (state.chatStatus in setOf("Disconnected", "Connecting", "Reconnecting")) modeStatusLabel(state.mode) else state.chatStatus
                                ConnectionState.CONNECTING -> "Connecting"
                                ConnectionState.RECONNECTING -> "Reconnecting"
                                ConnectionState.DISCONNECTED -> "Disconnected"
                            },
                        )
                    }
                }
            },
            onProtocolError = { message ->
                viewModelScope.launch {
                    _ui.update { it.copy(error = "Live update protocol error: $message") }
                    runCatching { loadBootstrapInternal() }
                        .onFailure { error ->
                            val message = if (error is Exception) friendlyError(error) else error.message ?: "Live state resync failed"
                            _ui.update { it.copy(error = message) }
                        }
                }
            },
            onSessionLost = { viewModelScope.launch { sessionLost("Controller session ended") } },
        ).also { it.connect() }
    }

    private suspend fun sessionLost(message: String) {
        webSocket?.close()
        webSocket = null
        recorder.cancel()
        cancelPendingPttStart()
        _ui.update { it.copy(session = null, connectionState = ConnectionState.DISCONNECTED, connectionLabel = "Disconnected", chatStatus = "Disconnected", screen = AppScreen.LOGIN, conversationActive = false, recording = false, notice = message) }
    }

    fun selectAgent(agentId: Int) = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        withContext(Dispatchers.IO) { localApi.activateAgent(token, agentId) }
        loadBootstrapInternal()
    }

    fun startConversation() = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        if (!_ui.value.connected) error("VerbaNode is not connected")
        cancelPtt()
        withContext(Dispatchers.IO) { localApi.startConversationMode(token) }
        loadBootstrapInternal()
        _ui.update { it.copy(notice = "Continuous conversation mode started.") }
    }

    fun newConversation() = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        withContext(Dispatchers.IO) { localApi.createConversation(token) }
        loadBootstrapInternal()
        _ui.update { it.copy(notice = "New chat created.") }
    }

    fun stopConversation() = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        recorder.cancel()
        pttStart?.let { if (!it.isCompleted) it.complete(false) }
        pttStart = null
        _ui.update { it.copy(recording = false) }
        withContext(Dispatchers.IO) {
            runCatching { localApi.cancelBrowserPtt(token) }
            localApi.stopConversationMode(token)
        }
        loadBootstrapInternal()
        _ui.update { it.copy(recording = false, notice = "Conversation mode stopped.") }
    }

    fun clearCurrentConversation() = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        val conversationId = _ui.value.conversationId ?: return@runBusy
        withContext(Dispatchers.IO) { localApi.clearConversation(token, conversationId) }
        refreshConversationInternal()
        _ui.update { it.copy(notice = "Conversation cleared.") }
    }

    fun stopTts() = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        withContext(Dispatchers.IO) { localApi.stopTts(token) }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _ui.update { it.copy(chatStatus = "Generating") }
        runBusy {
            val localApi = api ?: return@runBusy
            val token = _ui.value.session?.token ?: return@runBusy
            if (!_ui.value.connected) error("VerbaNode is not connected")
            withContext(Dispatchers.IO) { localApi.sendText(token, text.trim(), _ui.value.conversationId) }
            loadBootstrapInternal()
            refreshConversationInternal()
        }
    }

    private suspend fun refreshConversationInternal() {
        val localApi = api ?: return
        val token = _ui.value.session?.token ?: return
        val conversationId = _ui.value.conversationId ?: run {
            loadBootstrapInternal(); return
        }
        val messages = withContext(Dispatchers.IO) { localApi.conversation(token, conversationId) }
        _ui.update { it.copy(messages = messages) }
    }

    private fun cancelPendingPttStart() {
        pttStartJob?.cancel()
        pttStartJob = null
        pttStart?.let { if (!it.isCompleted) it.complete(false) }
        pttStart = null
    }

    fun startPtt() {
        if (!_ui.value.connected || _ui.value.session == null) {
            _ui.update { it.copy(error = "VerbaNode is not connected") }
            return
        }
        if (recorder.isRecording()) return
        val localApi = api ?: return
        val token = _ui.value.session?.token ?: return
        cancelPendingPttStart()
        val started = CompletableDeferred<Boolean>()
        pttStart = started
        try {
            recorder.start()
            _ui.update { it.copy(recording = true, chatStatus = "Recording", error = null) }
        } catch (error: Exception) {
            started.complete(false)
            _ui.update { it.copy(error = friendlyError(error), recording = false) }
            return
        }
        pttStartJob = viewModelScope.launch {
            try {
                withTimeout(PTT_START_TIMEOUT_MS) { localApi.startBrowserPttCancellable(token) }
                if (!started.isCompleted) started.complete(true)
            } catch (error: TimeoutCancellationException) {
                if (!started.isCompleted) started.complete(false)
                recorder.cancel()
                _ui.update {
                    it.copy(
                        error = "PTT start timed out. Check the connection and try again.",
                        recording = false,
                        chatStatus = modeStatusLabel(it.mode),
                    )
                }
                runCatching { withContext(Dispatchers.IO) { localApi.cancelBrowserPtt(token) } }
            } catch (error: CancellationException) {
                if (!started.isCompleted) started.complete(false)
                throw error
            } catch (error: Exception) {
                if (!started.isCompleted) started.complete(false)
                recorder.cancel()
                _ui.update { it.copy(error = friendlyError(error), recording = false, chatStatus = modeStatusLabel(it.mode)) }
                runCatching { withContext(Dispatchers.IO) { localApi.cancelBrowserPtt(token) } }
            }
        }
    }

    fun stopPtt() {
        if (!recorder.isRecording()) return
        val wav = recorder.stop()
        val startGate = pttStart
        val startJob = pttStartJob
        pttStart = null
        val operation = beginOperation("ptt-submit", clearFeedback = false)
        _ui.update { it.copy(recording = false, chatStatus = "Transcribing") }
        viewModelScope.launch {
            try {
                val localApi = api ?: return@launch
                val token = _ui.value.session?.token ?: return@launch
                val ready = if (startGate == null) {
                    false
                } else {
                    withTimeoutOrNull(PTT_START_GATE_TIMEOUT_MS) { startGate.await() } ?: false
                }
                if (!ready) startJob?.cancel()
                if (!ready || wav.size <= 44) {
                    withContext(Dispatchers.IO) { localApi.cancelBrowserPtt(token) }
                    _ui.update { it.copy(chatStatus = modeStatusLabel(it.mode)) }
                } else {
                    withContext(Dispatchers.IO) { localApi.submitBrowserPtt(token, wav) }
                    refreshConversationInternal()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _ui.update { it.copy(error = friendlyError(error), chatStatus = modeStatusLabel(it.mode)) }
            } finally {
                if (pttStartJob === startJob) pttStartJob = null
                endOperation(operation)
            }
        }
    }

    fun cancelPtt() {
        recorder.cancel()
        cancelPendingPttStart()
        _ui.update { it.copy(recording = false, chatStatus = modeStatusLabel(it.mode)) }
        val localApi = api ?: return
        val token = _ui.value.session?.token ?: return
        viewModelScope.launch(Dispatchers.IO) { runCatching { localApi.cancelBrowserPtt(token) } }
    }

    fun openDevices() = runBusy {
        loadDevicesInternal()
        _ui.update { it.copy(screen = AppScreen.DEVICES) }
    }

    fun startPairingManagement() = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        val pairing = withContext(Dispatchers.IO) { localApi.startPairing(token) }
        _ui.update { it.copy(pairingStatus = pairing, notice = "Pairing window opened.") }
    }

    private suspend fun loadDevicesInternal() {
        val localApi = api ?: return
        val token = _ui.value.session?.token ?: return
        val devices = withContext(Dispatchers.IO) { localApi.devices(token) }
        _ui.update { it.copy(devices = devices) }
    }

    fun renameDevice(deviceId: String, name: String) = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        withContext(Dispatchers.IO) { localApi.renameDevice(token, deviceId, name) }
        loadDevicesInternal()
    }

    fun revokeDevice(deviceId: String) = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        withContext(Dispatchers.IO) { localApi.revokeDevice(token, deviceId) }
        val current = _ui.value.currentProfile
        if (current?.deviceId == deviceId) {
            val unpaired = current.copy(deviceId = null, encryptedDeviceToken = null)
            store.upsert(unpaired)
            _ui.update { it.copy(currentProfile = unpaired) }
            sessionLost("This phone was revoked")
        } else loadDevicesInternal()
    }

    fun deleteDevice(deviceId: String) = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        withContext(Dispatchers.IO) { localApi.deleteDevice(token, deviceId) }
        loadDevicesInternal()
    }

    fun openStatus() = runBusy {
        val localApi = api ?: return@runBusy
        val token = _ui.value.session?.token ?: return@runBusy
        val status = withContext(Dispatchers.IO) { localApi.status(token) }
        _ui.update { it.copy(screen = AppScreen.STATUS, statusText = prettyStatus(status)) }
    }

    private fun requireApiSession(): ApiSessionContext {
        val localApi = api ?: error("Connect to VerbaNode first")
        val token = _ui.value.session?.token ?: error("Controller session is not active")
        return ApiSessionContext(localApi, token)
    }

    private suspend fun loadDashboardInternal() {
        val snapshot = management.dashboard()
        _ui.update {
            it.copy(
                dashboardStatus = snapshot.status,
                pipelineStatus = snapshot.pipeline,
                capabilityStatus = snapshot.capabilities,
            )
        }
    }

    fun openHome() = runBusy {
        loadDashboardInternal()
        _ui.update { it.copy(screen = AppScreen.HOME) }
    }

    fun openChat() {
        _ui.update { it.copy(screen = AppScreen.CHAT) }
        viewModelScope.launch { runCatching { refreshConversationInternal() } }
    }

    private suspend fun loadConfigurationOptionsInternal() {
        val options = management.configurationOptions()
        _ui.update { it.copy(configurationOptions = options) }
    }

    private suspend fun loadAgentsManagementInternal() {
        _ui.update { it.copy(rawAgents = management.agents()) }
    }

    fun openAgents() = runBusy { loadAgentsManagementInternal(); loadConfigurationOptionsInternal(); _ui.update { it.copy(screen = AppScreen.AGENTS) } }

    fun saveAgent(agentId: Int?, payload: JSONObject) = runBusy {
        val (localApi, token) = requireApiSession()
        withContext(Dispatchers.IO) {
            if (agentId == null) localApi.createAgent(token, payload) else localApi.updateAgent(token, agentId, payload)
        }
        loadBootstrapInternal(); loadAgentsManagementInternal()
        _ui.update { it.copy(notice = if (agentId == null) "Agent created." else "Agent saved.") }
    }

    fun deleteAgentManagement(agentId: Int) = runBusy {
        val (localApi, token) = requireApiSession()
        withContext(Dispatchers.IO) { localApi.deleteAgent(token, agentId) }
        loadBootstrapInternal(); loadAgentsManagementInternal()
        _ui.update { it.copy(notice = "Agent deleted.") }
    }

    fun clearAgentMemoryManagement(agentId: Int) = runBusy {
        val (localApi, token) = requireApiSession()
        withContext(Dispatchers.IO) { localApi.clearAgentMemory(token, agentId) }
        _ui.update { it.copy(notice = "Agent memory cleared.") }
    }

    fun exportAgent(agentId: Int, onReady: (ByteArray, String, String) -> Unit) = runBusy {
        val (localApi, token) = requireApiSession()
        val bytes = withContext(Dispatchers.IO) { localApi.agentBackup(token, agentId) }
        onReady(bytes, "verbanode-agent-$agentId.json", "application/json")
    }

    private suspend fun loadInformationInternal() {
        _ui.update { it.copy(informationItems = management.information()) }
    }

    fun openInformation() = runBusy { loadInformationInternal(); _ui.update { it.copy(screen = AppScreen.INFORMATION) } }

    fun saveInformation(id: Int?, title: String, content: String, enabled: Boolean) = runBusy {
        val (localApi, token) = requireApiSession()
        val payload = JSONObject().put("title", title.trim()).put("content", content.trim()).put("enabled", enabled)
        withContext(Dispatchers.IO) {
            if (id == null) localApi.createInformation(token, payload) else localApi.updateInformation(token, id, payload)
        }
        loadInformationInternal(); _ui.update { it.copy(notice = "Information saved.") }
    }

    fun deleteInformation(id: Int) = runBusy {
        val (localApi, token) = requireApiSession()
        withContext(Dispatchers.IO) { localApi.deleteInformation(token, id) }
        loadInformationInternal()
    }

    private suspend fun loadScriptsInternal() {
        val snapshot = management.scripts()
        _ui.update {
            it.copy(
                scriptItems = snapshot.scripts,
                queueItems = snapshot.queueItems,
                queueState = snapshot.queueState,
                queueLoop = snapshot.queueLoop,
                scriptDefaults = snapshot.defaults,
            )
        }
    }

    fun openScripts() = runBusy { loadScriptsInternal(); loadConfigurationOptionsInternal(); _ui.update { it.copy(screen = AppScreen.SCRIPTS) } }
    fun saveScriptDefaults(payload: JSONObject) = runBusy {
        val (localApi, token) = requireApiSession()
        val saved = withContext(Dispatchers.IO) { localApi.saveScriptDefaults(token, payload) }
        _ui.update { it.copy(scriptDefaults = saved, notice = "Script speech defaults saved.") }
    }


    fun saveScript(id: Int?, payload: JSONObject) = runBusy {
        val (localApi, token) = requireApiSession()
        withContext(Dispatchers.IO) {
            if (id == null) localApi.createScript(token, payload) else localApi.updateScript(token, id, payload)
        }
        loadScriptsInternal(); _ui.update { it.copy(notice = "Script saved.") }
    }

    fun deleteScript(id: Int) = runBusy { val (a,t)=requireApiSession(); withContext(Dispatchers.IO){a.deleteScript(t,id)}; loadScriptsInternal() }
    fun queueScript(id: Int) = runBusy { val (a,t)=requireApiSession(); withContext(Dispatchers.IO){a.queueScript(t,id)}; loadScriptsInternal() }
    fun runScriptNow(id: Int) = runBusy { val (a,t)=requireApiSession(); withContext(Dispatchers.IO){a.runScriptNow(t,id)}; _ui.update{it.copy(notice="Script started.")} }
    fun queueAction(action: String) = runBusy { val (a,t)=requireApiSession(); withContext(Dispatchers.IO){a.queueAction(t,action)}; loadScriptsInternal() }
    fun removeQueueItem(id: Int) = runBusy { val (a,t)=requireApiSession(); withContext(Dispatchers.IO){a.removeQueueItem(t,id)}; loadScriptsInternal() }
    fun setQueueLoop(enabled: Boolean) = runBusy {
        val (a,t)=requireApiSession(); withContext(Dispatchers.IO){a.setQueueLoop(t,enabled)}; loadScriptsInternal()
    }
    fun setQueuePause(id: Int, seconds: Double) = runBusy {
        val (a,t)=requireApiSession(); withContext(Dispatchers.IO){a.setQueuePause(t,id,seconds.coerceIn(0.0,3600.0))}; loadScriptsInternal()
    }
    fun moveQueueItem(id: Int, delta: Int) {
        val items = _ui.value.queueItems.toMutableList()
        val from = items.indexOfFirst { it.id == id }
        if (from < 0 || items.isEmpty()) return
        val to = (from + delta).coerceIn(0, items.lastIndex)
        if (to == from) return
        val moved = items.removeAt(from); items.add(to, moved)
        _ui.update { it.copy(queueItems = items) }
        viewModelScope.launch {
            try {
                val (a,t)=requireApiSession()
                withContext(Dispatchers.IO){a.reorderQueue(t, items.map { it.id })}
            } catch (error: Exception) {
                _ui.update { it.copy(error = friendlyError(error)) }
                runCatching { loadScriptsInternal() }
            }
        }
    }

    private suspend fun loadTypeToTalkInternal() {
        val snapshot = management.typeToTalk()
        _ui.update {
            it.copy(
                typeToTalkItems = snapshot.items,
                typeToTalkState = snapshot.state,
                typeToTalkSettings = snapshot.settings ?: it.typeToTalkSettings,
            )
        }
    }

    private suspend fun loadTypeToTalkVoiceOptionsInternal() {
        val options = management.typeToTalkVoiceOptions(_ui.value.configurationOptions) ?: return
        _ui.update { it.copy(configurationOptions = options) }
    }

    fun openTypeToTalk() = runBusy {
        loadTypeToTalkInternal()
        loadTypeToTalkVoiceOptionsInternal()
        _ui.update { it.copy(screen = AppScreen.TYPE_TO_TALK) }
    }
    fun addTypeToTalk(text: String, settings: JSONObject? = null) = runBusy {
        if (text.isBlank()) return@runBusy
        val (a,t)=requireApiSession()
        withContext(Dispatchers.IO){a.addTypeToTalk(t,text.trim(),settings)}
        if (settings != null) _ui.update { it.copy(typeToTalkSettings = JSONObject(settings.toString())) }
        loadTypeToTalkInternal()
    }
    fun saveTypeToTalkSettings(payload: JSONObject) = runBusy {
        val (a,t)=requireApiSession()
        val saved = withContext(Dispatchers.IO) { a.updateTypeToTalkSettings(t, payload) }
        _ui.update { it.copy(typeToTalkSettings = saved) }
    }
    fun typeToTalkAction(action: String) = runBusy {
        val (a,t)=requireApiSession(); withContext(Dispatchers.IO){ when(action){ "play" -> a.playTypeToTalk(t); "stop" -> a.stopTypeToTalk(t); "clear" -> a.clearTypeToTalk(t) } }; loadTypeToTalkInternal()
    }
    fun removeTypeToTalk(id: Int) = runBusy { val (a,t)=requireApiSession(); withContext(Dispatchers.IO){a.removeTypeToTalk(t,id)}; loadTypeToTalkInternal() }
    fun moveTypeToTalk(id: Int, delta: Int) {
        val items = _ui.value.typeToTalkItems.toMutableList()
        val from = items.indexOfFirst { it.id == id }
        if (from < 0 || items.isEmpty()) return
        val to = (from + delta).coerceIn(0, items.lastIndex)
        if (to == from) return
        val moved = items.removeAt(from); items.add(to, moved)
        _ui.update { it.copy(typeToTalkItems = items) }
        viewModelScope.launch {
            try { val (a,t)=requireApiSession(); withContext(Dispatchers.IO){a.reorderTypeToTalk(t, items.map { it.id })} }
            catch (error: Exception) { _ui.update { it.copy(error = friendlyError(error)) }; runCatching { loadTypeToTalkInternal() } }
        }
    }

    private suspend fun loadAudioLibraryInternal() {
        val snapshot = management.audioLibrary()
        _ui.update {
            it.copy(
                audioLibraryItems = snapshot.items,
                audioLibraryPlaying = snapshot.playing,
            )
        }
    }

    fun openAudio() = runBusy { loadAudioLibraryInternal(); _ui.update { it.copy(screen = AppScreen.AUDIO) } }
    fun uploadAudio(uri: Uri, filename: String, mimeType: String, contentLength: Long?) = runBusy("audio-upload") {
        val (a,t)=requireApiSession()
        val resolver = getApplication<Application>().contentResolver
        withContext(Dispatchers.IO) {
            a.uploadAudio(t, filename, mimeType, contentLength) {
                resolver.openInputStream(uri) ?: throw IOException("Could not open audio file")
            }
        }
        loadAudioLibraryInternal()
        _ui.update{it.copy(notice="Audio uploaded.")}
    }
    fun playAudio(name: String) = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.playAudio(t,name)}; loadAudioLibraryInternal() }
    fun stopAudio() = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.stopAudio(t)}; loadAudioLibraryInternal() }
    fun renameAudio(name: String, newName: String) = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.renameAudio(t,name,newName)}; loadAudioLibraryInternal() }
    fun deleteAudio(name: String) = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.deleteAudio(t,name)}; loadAudioLibraryInternal() }
    fun setChatAutoScroll(enabled: Boolean) = _ui.update { it.copy(chatAutoScroll = enabled) }

    private suspend fun loadPluginsInternal() {
        val snapshot = management.plugins()
        _ui.update { it.copy(pluginItems = snapshot.items, pluginSummary = snapshot.summary) }
    }

    fun openPlugins() = runBusy { loadPluginsInternal(); _ui.update { it.copy(screen = AppScreen.PLUGINS) } }
    fun refreshPlugins() = runBusy { loadPluginsInternal(); _ui.update { it.copy(notice = "Plugin status refreshed.") } }
    fun setPluginEnabled(id: String, enabled: Boolean) = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.setPluginEnabled(t,id,enabled)}; loadPluginsInternal() }
    fun reloadPlugins() = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.reloadPlugins(t)}; loadPluginsInternal(); _ui.update { it.copy(notice = "External plugins reloaded.") } }
    fun reloadPlugin(id: String) = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.reloadPlugin(t,id)}; loadPluginsInternal() }
    fun recoverPlugin(id: String) = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.recoverPlugin(t,id)}; loadPluginsInternal() }
    fun resetPluginMetrics(id: String? = null) = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.resetPluginMetrics(t,id)}; loadPluginsInternal() }

    private suspend fun loadSettingsInternal() {
        val snapshot = management.settings()
        _ui.update {
            it.copy(
                runtimeSettings = snapshot.runtimeSettings,
                audioDevices = snapshot.audioDevices,
                modelItems = snapshot.models,
                dashboardStatus = snapshot.bootstrap,
            )
        }
    }

    fun openSettings() = runBusy { loadSettingsInternal(); _ui.update { it.copy(screen = AppScreen.SETTINGS) } }

    fun saveConversationSettings(payload: JSONObject) = runBusy {
        val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.saveConversationSettings(t,payload)}; loadSettingsInternal(); _ui.update{it.copy(notice="Settings saved.")}
    }
    fun refreshAudioDevices() = runBusy { val(a,t)=requireApiSession(); val value=withContext(Dispatchers.IO){a.refreshAudio(t)}; _ui.update{it.copy(audioDevices=value)} }
    fun restartAudioEngine() = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.restartAudio(t)}; loadDashboardInternal(); _ui.update{it.copy(notice="Audio Engine restart requested.")} }
    fun testAudio(endpoint: String, inputId: Int?, outputId: Int?) = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.testAudio(t,endpoint,inputId,outputId)}; _ui.update{it.copy(notice="Audio test completed.")} }
    fun restartAiEngine() = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.restartAi(t)}; _ui.update{it.copy(notice="AI Engine restart requested.")} }
    fun reloadAsr() = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.reloadAsr(t)}; _ui.update{it.copy(notice="ASR reload requested.")} }
    fun reloadKokoro() = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.reloadKokoro(t)}; _ui.update{it.copy(notice="Kokoro reload requested.")} }
    fun pullModel(name: String) = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.pullModel(t,name.trim())}; _ui.update{it.copy(notice="Model pull started. Progress will arrive over WebSocket.")} }

    fun openDiagnostics() = runBusy {
        val(a,t)=requireApiSession(); val value=withContext(Dispatchers.IO){a.diagnostics(t)}
        _ui.update{it.copy(screen=AppScreen.DIAGNOSTICS, diagnosticsStatus=value)}
    }
    fun runDiagnosticsSelfTest() = runBusy { val(a,t)=requireApiSession(); val value=withContext(Dispatchers.IO){a.runSelfTest(t)}; _ui.update{it.copy(diagnosticsStatus=(it.diagnosticsStatus ?: JSONObject()).put("self_test",value), notice="Self-test complete.")} }
    fun clearDiagnosticLogs() = runBusy { val(a,t)=requireApiSession(); withContext(Dispatchers.IO){a.clearDiagnosticLogs(t)}; openDiagnosticsDirect() }
    private suspend fun openDiagnosticsDirect() { val(a,t)=requireApiSession(); _ui.update{it.copy(diagnosticsStatus=withContext(Dispatchers.IO){a.diagnostics(t)})} }
    fun exportDiagnostics(onReady:(File,String,String)->Unit) = runBusy("diagnostics-export") {
        val(a,t)=requireApiSession()
        val file=withContext(Dispatchers.IO) {
            val target=File.createTempFile("verbanode-diagnostics-", ".zip", getApplication<Application>().cacheDir)
            try { a.diagnosticsExportTo(t,target) } catch (error: Exception) { target.delete(); throw error }
        }
        onReady(file,"verbanode-diagnostics.zip","application/zip")
    }

    fun openData() = runBusy {
        val(a,t)=requireApiSession(); val value=withContext(Dispatchers.IO){a.backupStatus(t)}
        _ui.update{it.copy(screen=AppScreen.DATA, backupStatus=value)}
    }
    fun exportBackup(onReady:(File,String,String)->Unit) = runBusy("backup-export") {
        val(a,t)=requireApiSession()
        val file=withContext(Dispatchers.IO) {
            val target=File.createTempFile("verbanode-backup-", ".zip", getApplication<Application>().cacheDir)
            try { a.downloadBackupTo(t,target) } catch (error: Exception) { target.delete(); throw error }
        }
        onReady(file,"verbanode-backup.zip","application/zip")
    }
    fun restoreBackup(uri: Uri, filename: String, contentLength: Long?) = runBusy("backup-restore") {
        val(a,t)=requireApiSession()
        val resolver = getApplication<Application>().contentResolver
        withContext(Dispatchers.IO) {
            a.restoreBackup(t, filename, contentLength) {
                resolver.openInputStream(uri) ?: throw IOException("Could not open backup")
            }
        }
        _ui.update{it.copy(notice="Backup restored. Restart VerbaNode Core before continuing.")}
    }

    fun backHome() = openHome()

    fun logout() {
        val localApi = api
        val token = _ui.value.session?.token
        webSocket?.close(); webSocket = null
        recorder.cancel()
        cancelPendingPttStart()
        if (localApi != null && token != null) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { localApi.cancelBrowserPtt(token) }
                runCatching { localApi.logout(token) }
            }
        }
        _ui.update { it.copy(session = null, connectionState = ConnectionState.DISCONNECTED, connectionLabel = "Disconnected", screen = AppScreen.LOGIN, messages = emptyList(), conversationActive = false, recording = false) }
    }

    fun removeProfile(profile: ServerProfile) = viewModelScope.launch {
        store.remove(profile.id)
        val profiles = store.profiles()
        _ui.update { it.copy(profiles = profiles) }
    }

    fun goServers() {
        val localApi = api
        val token = _ui.value.session?.token
        webSocket?.close(); webSocket = null
        recorder.cancel()
        cancelPendingPttStart()
        if (localApi != null && token != null) {
            viewModelScope.launch(Dispatchers.IO) { runCatching { localApi.cancelBrowserPtt(token) } }
        }
        api = null
        _ui.update { it.copy(screen = AppScreen.SERVERS, session = null, currentProfile = null, connectionState = ConnectionState.DISCONNECTED, connectionLabel = "Disconnected", conversationActive = false, recording = false) }
    }

    override fun onCleared() {
        stopDiscovery(clearResults = false)
        webSocket?.close()
        recorder.cancel()
        cancelPendingPttStart()
        super.onCleared()
    }
}
