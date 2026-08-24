package com.verbanode.mobile.network

import android.os.Build
import com.verbanode.mobile.BuildConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine

class VerbaNodeApi(
    val baseUrl: String,
    val spkiSha256: String,
    val client: OkHttpClient = TlsTrust.pinnedClient(spkiSha256),
) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private fun pathSegment(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun buildRequest(
        path: String,
        method: String = "GET",
        sessionToken: String? = null,
        json: JSONObject? = null,
        body: RequestBody? = null,
    ): Request {
        val builder = Request.Builder().url(baseUrl.removeSuffix("/") + path)
        if (!sessionToken.isNullOrBlank()) builder.header("X-Session-Token", sessionToken)
        val requestBody = body ?: json?.toString()?.toRequestBody(jsonMedia)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody(null))
            "PATCH" -> builder.patch(requestBody ?: ByteArray(0).toRequestBody(null))
            "PUT" -> builder.put(requestBody ?: ByteArray(0).toRequestBody(null))
            "DELETE" -> if (requestBody == null) builder.delete() else builder.delete(requestBody)
            else -> error("Unsupported HTTP method $method")
        }
        return builder.build()
    }

    private fun errorFrom(status: Int, text: String): ApiException {
        val payload = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
        val error = payload.optJSONObject("error")
        val message = error?.optString("message")?.takeIf { it.isNotBlank() }
            ?: payload.optString("detail").takeIf { it.isNotBlank() }
            ?: "Request failed ($status)"
        return ApiException(status, error?.optString("code")?.ifBlank { null }, message)
    }

    private fun requestText(
        path: String,
        method: String = "GET",
        sessionToken: String? = null,
        json: JSONObject? = null,
        body: RequestBody? = null,
        timeoutSeconds: Long? = null,
    ): String {
        val call = client.newCall(buildRequest(path, method, sessionToken, json, body))
        timeoutSeconds?.let { call.timeout().timeout(it, TimeUnit.SECONDS) }
        call.execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) throw errorFrom(response.code, text)
            return text
        }
    }

    private fun request(
        path: String,
        method: String = "GET",
        sessionToken: String? = null,
        json: JSONObject? = null,
        body: RequestBody? = null,
    ): JSONObject {
        val text = requestText(path, method, sessionToken, json, body)
        if (text.isBlank()) return JSONObject()
        return try {
            JSONObject(text)
        } catch (error: Exception) {
            throw ApiProtocolException("VerbaNode returned malformed JSON for $path", error)
        }
    }

    private fun requestArray(
        path: String,
        method: String = "GET",
        sessionToken: String? = null,
        json: JSONObject? = null,
    ): JSONArray {
        val text = requestText(path, method, sessionToken, json)
        if (text.isBlank()) return JSONArray()
        return try {
            JSONArray(text)
        } catch (error: Exception) {
            throw ApiProtocolException("VerbaNode returned malformed JSON array for $path", error)
        }
    }

    private suspend fun requestUnitCancellable(
        path: String,
        method: String,
        sessionToken: String,
        timeoutSeconds: Long,
    ) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val call = client.newCall(buildRequest(path, method, sessionToken))
            call.timeout().timeout(timeoutSeconds, TimeUnit.SECONDS)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    continuation.resumeWith(Result.failure(error))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            val text = it.body.string()
                            if (!it.isSuccessful) throw errorFrom(it.code, text)
                        }
                        continuation.resumeWith(Result.success(Unit))
                    } catch (error: Exception) {
                        continuation.resumeWith(Result.failure(error))
                    }
                }
            })
        }
    }

    private fun requestBytes(path: String, sessionToken: String): ByteArray {
        client.newCall(buildRequest(path, sessionToken = sessionToken)).execute().use { response ->
            if (!response.isSuccessful) {
                val text = response.body.string()
                throw errorFrom(response.code, text)
            }
            return response.body.bytes()
        }
    }

    private fun requestToFile(path: String, sessionToken: String, destination: File): File {
        client.newCall(buildRequest(path, sessionToken = sessionToken)).execute().use { response ->
            if (!response.isSuccessful) {
                val text = response.body.string()
                throw errorFrom(response.code, text)
            }
            destination.parentFile?.mkdirs()
            FileOutputStream(destination).use { output ->
                response.body.byteStream().use { input -> input.copyTo(output, DEFAULT_BUFFER_SIZE) }
            }
            return destination
        }
    }

    private fun streamingBody(
        mimeType: String,
        contentLength: Long?,
        openStream: () -> InputStream,
    ): RequestBody = object : RequestBody() {
        private val mediaType = mimeType.toMediaType()
        override fun contentType() = mediaType
        override fun contentLength(): Long = contentLength?.takeIf { it >= 0L } ?: -1L
        override fun writeTo(sink: BufferedSink) {
            openStream().use { input -> sink.writeAll(input.source()) }
        }
    }

    fun clientInfo(): ClientInfo = parseClientInfo(request("/api/client-info"))

    fun pinLogin(pin: String, deviceName: String): AuthSession {
        val payload = request(
            "/api/auth/login",
            method = "POST",
            json = JSONObject()
                .put("pin", pin)
                .put("client_name", deviceName)
                .put("client_type", "mobile")
                .put("client_version", BuildConfig.VERSION_NAME)
                .put("api_version", 1),
        )
        return parseAuth(payload)
    }

    fun deviceLogin(deviceId: String, deviceToken: String, deviceName: String): AuthSession {
        val payload = request(
            "/api/auth/device-login",
            method = "POST",
            json = JSONObject()
                .put("device_id", deviceId)
                .put("device_token", deviceToken)
                .put("client_name", deviceName)
                .put("client_type", "mobile")
                .put("client_version", BuildConfig.VERSION_NAME)
                .put("api_version", 1),
        )
        return parseAuth(payload)
    }

    private fun parseAuth(payload: JSONObject): AuthSession {
        val token = payload.optString("token")
        if (token.isBlank()) error("VerbaNode did not return a controller session")
        val session = payload.optJSONObject("session") ?: JSONObject()
        return AuthSession(
            token = token,
            sessionId = session.optString("session_id").ifBlank { null },
            clientName = session.optString("client_name").ifBlank { null },
            deviceId = session.optString("device_id").ifBlank { null },
        )
    }

    fun logout(sessionToken: String) { request("/api/auth/logout", method = "POST", sessionToken = sessionToken) }
    fun bootstrap(sessionToken: String): BootstrapData = parseBootstrap(request("/api/bootstrap", sessionToken = sessionToken))
    fun bootstrapRaw(sessionToken: String): JSONObject = request("/api/bootstrap", sessionToken = sessionToken)

    fun activateAgent(sessionToken: String, agentId: Int) { request("/api/agents/$agentId/activate", method = "POST", sessionToken = sessionToken) }

    fun conversation(sessionToken: String, conversationId: Int): List<ChatMessage> {
        val payload = request("/api/conversations/$conversationId", sessionToken = sessionToken)
        val array = payload.optJSONArray("messages") ?: JSONArray()
        return buildList { for (index in 0 until array.length()) array.optJSONObject(index)?.let { add(parseMessage(it)) } }
    }

    fun sendText(sessionToken: String, text: String, conversationId: Int?): JSONObject = request(
        "/api/chat/send", method = "POST", sessionToken = sessionToken,
        json = JSONObject().put("text", text).apply { if (conversationId != null) put("conversation_id", conversationId) },
    )

    fun createConversation(sessionToken: String, title: String? = null): JSONObject = request(
        "/api/conversations", method = "POST", sessionToken = sessionToken,
        json = JSONObject().apply { if (!title.isNullOrBlank()) put("title", title) },
    )

    fun startConversationMode(sessionToken: String): JSONObject =
        request("/api/conversation/start", method = "POST", sessionToken = sessionToken)

    fun stopConversationMode(sessionToken: String): JSONObject =
        request("/api/conversation/stop", method = "POST", sessionToken = sessionToken)

    fun clearConversation(sessionToken: String, conversationId: Int) {
        request("/api/conversations/$conversationId/messages", method = "DELETE", sessionToken = sessionToken)
    }

    fun listConversations(sessionToken: String, agentId: Int): JSONArray = requestArray("/api/agents/$agentId/conversations", sessionToken = sessionToken)

    suspend fun startBrowserPttCancellable(sessionToken: String) {
        requestUnitCancellable("/api/browser-ptt/start", "POST", sessionToken, timeoutSeconds = 8)
    }
    fun cancelBrowserPtt(sessionToken: String) {
        requestText("/api/browser-ptt/cancel", method = "POST", sessionToken = sessionToken, timeoutSeconds = 5)
    }
    fun stopTts(sessionToken: String) { request("/api/tts/stop", method = "POST", sessionToken = sessionToken) }

    fun submitBrowserPtt(sessionToken: String, wav: ByteArray): JSONObject {
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "android-ptt.wav", wav.toRequestBody("audio/wav".toMediaType())).build()
        return request("/api/browser-ptt/audio", method = "POST", sessionToken = sessionToken, body = multipart)
    }

    fun startPairing(sessionToken: String): JSONObject = request(
        "/api/devices/pairing/start", method = "POST", sessionToken = sessionToken,
        json = JSONObject().put("preferred_server_url", baseUrl),
    )

    fun claimPairing(pairingId: String?, secret: String?, shortCode: String?, deviceName: String): PairingClaim {
        val json = JSONObject().put("device_name", deviceName).put("device_type", "mobile")
            .put("device_version", BuildConfig.VERSION_NAME).put("platform", "android-${Build.VERSION.SDK_INT}")
        if (!pairingId.isNullOrBlank()) json.put("pairing_id", pairingId)
        if (!secret.isNullOrBlank()) json.put("secret", secret)
        if (!shortCode.isNullOrBlank()) json.put("short_code", shortCode)
        val payload = request("/api/pairing/claim", method = "POST", json = json)
        return PairingClaim(
            deviceId = payload.getString("device_id"), deviceToken = payload.getString("device_token"),
            deviceName = payload.optString("device_name", deviceName), serverUrl = payload.optString("server_url", baseUrl),
            spkiSha256 = payload.optString("certificate_spki_sha256", spkiSha256).lowercase(),
        )
    }

    fun devices(sessionToken: String): List<TrustedDevice> = parseDevices(request("/api/devices", sessionToken = sessionToken))
    fun renameDevice(sessionToken: String, deviceId: String, name: String) {
        request("/api/devices/$deviceId", method = "PATCH", sessionToken = sessionToken, json = JSONObject().put("name", name))
    }
    fun revokeDevice(sessionToken: String, deviceId: String) { request("/api/devices/$deviceId/revoke", method = "POST", sessionToken = sessionToken) }
    fun deleteDevice(sessionToken: String, deviceId: String) { request("/api/devices/$deviceId", method = "DELETE", sessionToken = sessionToken) }

    fun status(sessionToken: String): JSONObject = request("/api/status", sessionToken = sessionToken)
    fun pipeline(sessionToken: String): JSONObject = request("/api/pipeline", sessionToken = sessionToken)
    fun capabilities(sessionToken: String): JSONObject = request("/api/capabilities", sessionToken = sessionToken)
    fun actions(sessionToken: String): JSONObject = request("/api/actions", sessionToken = sessionToken)

    fun agentsRaw(sessionToken: String): JSONArray = requestArray("/api/agents", sessionToken = sessionToken)
    fun createAgent(sessionToken: String, payload: JSONObject): JSONObject = request("/api/agents", "POST", sessionToken, payload)
    fun updateAgent(sessionToken: String, agentId: Int, payload: JSONObject): JSONObject = request("/api/agents/$agentId", "PUT", sessionToken, payload)
    fun deleteAgent(sessionToken: String, agentId: Int) { request("/api/agents/$agentId", "DELETE", sessionToken) }
    fun clearAgentMemory(sessionToken: String, agentId: Int) { request("/api/agents/$agentId/memory", "DELETE", sessionToken) }
    fun agentBackup(sessionToken: String, agentId: Int): ByteArray = requestBytes("/api/agents/$agentId/backup", sessionToken)

    fun information(sessionToken: String): JSONArray = requestArray("/api/information", sessionToken = sessionToken)
    fun createInformation(sessionToken: String, payload: JSONObject): JSONObject = request("/api/information", "POST", sessionToken, payload)
    fun updateInformation(sessionToken: String, id: Int, payload: JSONObject): JSONObject = request("/api/information/$id", "PUT", sessionToken, payload)
    fun deleteInformation(sessionToken: String, id: Int) { request("/api/information/$id", "DELETE", sessionToken) }

    fun scriptDefaults(sessionToken: String): JSONObject = request("/api/scripts/defaults", sessionToken = sessionToken)
    fun saveScriptDefaults(sessionToken: String, payload: JSONObject): JSONObject = request("/api/scripts/defaults", "PUT", sessionToken, payload)

    fun scripts(sessionToken: String): JSONArray = requestArray("/api/scripts", sessionToken = sessionToken)
    fun createScript(sessionToken: String, payload: JSONObject): JSONObject = request("/api/scripts", "POST", sessionToken, payload)
    fun updateScript(sessionToken: String, id: Int, payload: JSONObject): JSONObject = request("/api/scripts/$id", "PUT", sessionToken, payload)
    fun deleteScript(sessionToken: String, id: Int) { request("/api/scripts/$id", "DELETE", sessionToken) }
    fun queueScript(sessionToken: String, id: Int) { request("/api/scripts/$id/queue", "POST", sessionToken) }
    fun runScriptNow(sessionToken: String, id: Int) { request("/api/scripts/$id/run-now", "POST", sessionToken) }
    fun queue(sessionToken: String): JSONObject = request("/api/queue", sessionToken = sessionToken)
    fun queueAction(sessionToken: String, action: String) {
        val method = if (action == "clear") "DELETE" else "POST"
        val path = if (action == "clear") "/api/queue" else "/api/queue/$action"
        request(path, method, sessionToken)
    }
    fun removeQueueItem(sessionToken: String, id: Int) { request("/api/queue/$id", "DELETE", sessionToken) }
    fun reorderQueue(sessionToken: String, orderedIds: List<Int>) {
        val array = JSONArray(); orderedIds.forEach { array.put(it) }
        request("/api/queue/reorder", "PUT", sessionToken, JSONObject().put("ordered_ids", array))
    }

    fun typeToTalk(sessionToken: String): JSONObject = request("/api/type-to-talk", sessionToken = sessionToken)
    fun addTypeToTalk(sessionToken: String, text: String, settings: JSONObject? = null): JSONObject {
        val payload = settings?.let { JSONObject(it.toString()) } ?: JSONObject()
        payload.put("text", text)
        return request("/api/type-to-talk", "POST", sessionToken, payload)
    }
    fun updateTypeToTalkSettings(sessionToken: String, payload: JSONObject): JSONObject = request("/api/type-to-talk/settings", "PATCH", sessionToken, payload)
    fun playTypeToTalk(sessionToken: String) { request("/api/type-to-talk/play", "POST", sessionToken) }
    fun stopTypeToTalk(sessionToken: String) { request("/api/type-to-talk/stop", "POST", sessionToken) }
    fun clearTypeToTalk(sessionToken: String) { request("/api/type-to-talk", "DELETE", sessionToken) }
    fun removeTypeToTalk(sessionToken: String, id: Int) { request("/api/type-to-talk/$id", "DELETE", sessionToken) }
    fun reorderTypeToTalk(sessionToken: String, orderedIds: List<Int>) {
        val array = JSONArray(); orderedIds.forEach { array.put(it) }
        request("/api/type-to-talk/reorder", "PUT", sessionToken, JSONObject().put("ordered_ids", array))
    }

    fun plugins(sessionToken: String): JSONObject = request("/api/plugins", sessionToken = sessionToken)
    fun setPluginEnabled(sessionToken: String, id: String, enabled: Boolean): JSONObject = request(
        "/api/plugins/$id", "PUT", sessionToken, JSONObject().put("enabled", enabled),
    )
    fun reloadPlugins(sessionToken: String): JSONObject = request("/api/plugins/reload", "POST", sessionToken)
    fun reloadPlugin(sessionToken: String, id: String): JSONObject = request("/api/plugins/$id/reload", "POST", sessionToken)
    fun recoverPlugin(sessionToken: String, id: String): JSONObject = request("/api/plugins/$id/recover", "POST", sessionToken)
    fun resetPluginMetrics(sessionToken: String, id: String? = null): JSONObject = request(
        if (id == null) "/api/plugins/reset-metrics" else "/api/plugins/$id/reset-metrics", "POST", sessionToken,
    )

    fun models(sessionToken: String): JSONArray = requestArray("/api/models", sessionToken = sessionToken)
    fun pullModel(sessionToken: String, name: String) { request("/api/models/pull/${name.replace(" ", "%20")}", "POST", sessionToken) }
    fun restartAi(sessionToken: String) { request("/api/ai/restart-engine", "POST", sessionToken) }
    fun reloadAsr(sessionToken: String) { request("/api/ai/reload-asr", "POST", sessionToken) }
    fun reloadKokoro(sessionToken: String) { request("/api/ai/reload-kokoro", "POST", sessionToken) }

    fun audioDevices(sessionToken: String): JSONObject = request("/api/audio/devices", sessionToken = sessionToken)
    fun refreshAudio(sessionToken: String): JSONObject = request("/api/audio/refresh", "POST", sessionToken)
    fun restartAudio(sessionToken: String) { request("/api/audio/restart-engine", "POST", sessionToken) }
    fun testAudio(sessionToken: String, endpoint: String, inputId: Int?, outputId: Int?): JSONObject = request(
        "/api/audio/$endpoint", "POST", sessionToken,
        JSONObject().apply { put("input_device", inputId ?: JSONObject.NULL); put("output_device", outputId ?: JSONObject.NULL) },
    )
    fun saveConversationSettings(sessionToken: String, payload: JSONObject): JSONObject = request(
        "/api/conversation/settings", "PUT", sessionToken, payload,
    )

    fun diagnostics(sessionToken: String): JSONObject = request("/api/diagnostics", sessionToken = sessionToken)
    fun runSelfTest(sessionToken: String): JSONObject = request("/api/diagnostics/self-test", "POST", sessionToken)
    fun clearDiagnosticLogs(sessionToken: String) { request("/api/diagnostics/logs", "DELETE", sessionToken) }
    fun clearDiagnosticTurns(sessionToken: String) { request("/api/diagnostics/turns", "DELETE", sessionToken) }
    fun diagnosticsExportTo(sessionToken: String, destination: File): File =
        requestToFile("/api/diagnostics/export", sessionToken, destination)

    fun backupStatus(sessionToken: String): JSONObject = request("/api/backup/status", sessionToken = sessionToken)
    fun downloadBackupTo(sessionToken: String, destination: File): File =
        requestToFile("/api/backup", sessionToken, destination)
    fun restoreBackup(
        sessionToken: String,
        filename: String,
        contentLength: Long?,
        openStream: () -> InputStream,
    ): JSONObject {
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, streamingBody("application/zip", contentLength, openStream)).build()
        return request("/api/restore", "POST", sessionToken, body = multipart)
    }


    fun configurationOptions(sessionToken: String): JSONObject = request("/api/configuration-options", sessionToken = sessionToken)

    fun edgeVoices(sessionToken: String, refresh: Boolean = false): JSONObject = request(
        "/api/tts/edge-voices?refresh=$refresh", sessionToken = sessionToken,
    )

    fun audioLibrary(sessionToken: String): JSONObject = request("/api/audio-library", sessionToken = sessionToken)
    fun uploadAudio(
        sessionToken: String,
        filename: String,
        mimeType: String,
        contentLength: Long?,
        openStream: () -> InputStream,
    ): JSONObject {
        val media = mimeType.takeIf { it.isNotBlank() && '*' !in it }
            ?: if (filename.lowercase().endsWith(".mp3")) "audio/mpeg" else "application/octet-stream"
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, streamingBody(media, contentLength, openStream)).build()
        return request("/api/audio-library/upload", "POST", sessionToken, body = multipart)
    }
    fun playAudio(sessionToken: String, name: String): JSONObject = request("/api/audio-library/${pathSegment(name)}/play", "POST", sessionToken)
    fun stopAudio(sessionToken: String): JSONObject = request("/api/audio-library/stop", "POST", sessionToken)
    fun renameAudio(sessionToken: String, name: String, newName: String): JSONObject = request(
        "/api/audio-library/${pathSegment(name)}", "PATCH", sessionToken, JSONObject().put("name", newName),
    )
    fun deleteAudio(sessionToken: String, name: String) { request("/api/audio-library/${pathSegment(name)}", "DELETE", sessionToken) }

    fun setQueueLoop(sessionToken: String, loop: Boolean): JSONObject = request(
        "/api/queue/settings", "PUT", sessionToken, JSONObject().put("loop", loop),
    )
    fun setQueuePause(sessionToken: String, queueId: Int, seconds: Double): JSONObject = request(
        "/api/queue/$queueId", "PATCH", sessionToken, JSONObject().put("pause_after_seconds", seconds),
    )

    fun wsTicket(sessionToken: String): String {
        val text = requestText(
            "/api/auth/ws-ticket",
            method = "POST",
            sessionToken = sessionToken,
            timeoutSeconds = 8,
        )
        return try {
            JSONObject(text).getString("ticket")
        } catch (error: Exception) {
            throw ApiProtocolException("VerbaNode returned an invalid WebSocket ticket response", error)
        }
    }
}
