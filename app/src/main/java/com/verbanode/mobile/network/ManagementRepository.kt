package com.verbanode.mobile.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Authenticated Core connection used by management repositories.
 *
 * Keeping API/session access in one value avoids passing a raw token through
 * every state-loading path and gives future repositories a stable boundary.
 */
data class ApiSessionContext(
    val api: VerbaNodeApi,
    val token: String,
)

data class DashboardSnapshot(
    val status: JSONObject,
    val pipeline: JSONObject,
    val capabilities: JSONObject,
)

data class ScriptsSnapshot(
    val scripts: List<ScriptItem>,
    val queueItems: List<ScriptQueueItem>,
    val queueState: String,
    val queueLoop: Boolean,
    val defaults: JSONObject,
)

data class TypeToTalkSnapshot(
    val items: List<TypeToTalkItem>,
    val state: String,
    val settings: JSONObject?,
)

data class AudioLibrarySnapshot(
    val items: List<AudioLibraryItem>,
    val playing: String?,
)

data class PluginSnapshot(
    val items: List<JSONObject>,
    val summary: JSONObject?,
)

data class SettingsSnapshot(
    val runtimeSettings: JSONObject?,
    val audioDevices: JSONObject,
    val models: List<JSONObject>,
    val bootstrap: JSONObject,
)

/**
 * Read-side management boundary between AppViewModel and the REST client.
 *
 * All blocking OkHttp work and response normalization lives here. The
 * ViewModel remains responsible for navigation and UI state transitions, while
 * this repository owns feature snapshots and protocol-to-model conversion.
 */
class ManagementRepository(
    private val sessionProvider: () -> ApiSessionContext,
) {
    private fun JSONArray.objects(): List<JSONObject> = buildList {
        for (index in 0 until length()) optJSONObject(index)?.let(::add)
    }

    suspend fun dashboard(): DashboardSnapshot {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) {
            DashboardSnapshot(
                status = api.status(token),
                pipeline = api.pipeline(token),
                capabilities = api.capabilities(token),
            )
        }
    }

    suspend fun configurationOptions(): JSONObject {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) {
            val options = api.configurationOptions(token)
            val liveModels = runCatching { api.models(token) }.getOrElse { JSONArray() }
            val merged = linkedSetOf<String>()
            val configured = options.optJSONArray("llm_models") ?: JSONArray()
            for (index in 0 until configured.length()) {
                configured.optString(index).trim().takeIf { it.isNotBlank() }?.let(merged::add)
            }
            for (index in 0 until liveModels.length()) {
                val item = liveModels.optJSONObject(index) ?: continue
                item.optString("name", item.optString("model")).trim()
                    .takeIf { it.isNotBlank() }
                    ?.let(merged::add)
            }
            val modelArray = JSONArray()
            merged.forEach(modelArray::put)
            options.put("llm_models", modelArray)

            runCatching { api.edgeVoices(token) }.getOrNull()?.let { payload ->
                payload.optJSONArray("voices")?.let { options.put("edge_voices", it) }
                options.put("edge_voices_source", payload.optString("source"))
            }
            options
        }
    }

    suspend fun agents(): List<JSONObject> {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) { api.agentsRaw(token).objects() }
    }

    suspend fun information(): List<JSONObject> {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) { api.information(token).objects() }
    }

    suspend fun scripts(): ScriptsSnapshot {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) {
            val scripts = parseScriptItems(api.scripts(token))
            val defaults = api.scriptDefaults(token)
            val queue = api.queue(token)
            ScriptsSnapshot(
                scripts = scripts,
                queueItems = parseScriptQueueItems(queue.optJSONArray("items") ?: JSONArray()),
                queueState = queue.optString("state", "paused"),
                queueLoop = queue.optBoolean("loop", false),
                defaults = defaults,
            )
        }
    }

    suspend fun typeToTalk(): TypeToTalkSnapshot {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) {
            val payload = api.typeToTalk(token)
            TypeToTalkSnapshot(
                items = parseTypeToTalkItems(payload.optJSONArray("items") ?: JSONArray()),
                state = payload.optString("state", "idle"),
                settings = payload.optJSONObject("settings"),
            )
        }
    }

    suspend fun typeToTalkVoiceOptions(current: JSONObject?): JSONObject? {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) {
            val payload = runCatching { api.edgeVoices(token) }.getOrNull() ?: return@withContext null
            val voices = payload.optJSONArray("voices") ?: return@withContext null
            JSONObject((current ?: JSONObject()).toString())
                .put("edge_voices", voices)
                .put("edge_voices_source", payload.optString("source"))
        }
    }

    suspend fun audioLibrary(): AudioLibrarySnapshot {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) {
            val payload = api.audioLibrary(token)
            AudioLibrarySnapshot(
                items = parseAudioLibraryItems(payload.optJSONArray("items") ?: JSONArray()),
                playing = payload.optString("playing").ifBlank { null },
            )
        }
    }

    suspend fun plugins(): PluginSnapshot {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) {
            val payload = api.plugins(token)
            PluginSnapshot(
                items = (payload.optJSONArray("plugins") ?: JSONArray()).objects(),
                summary = payload.optJSONObject("summary"),
            )
        }
    }

    suspend fun settings(): SettingsSnapshot {
        val (api, token) = sessionProvider()
        return withContext(Dispatchers.IO) {
            val bootstrap = api.bootstrapRaw(token)
            SettingsSnapshot(
                runtimeSettings = bootstrap.optJSONObject("runtime_settings"),
                audioDevices = api.audioDevices(token),
                models = runCatching { api.models(token).objects() }.getOrElse { emptyList() },
                bootstrap = bootstrap,
            )
        }
    }
}
