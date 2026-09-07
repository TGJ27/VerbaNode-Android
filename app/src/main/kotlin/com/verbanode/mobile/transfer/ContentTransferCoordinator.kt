package com.verbanode.mobile.transfer

import android.content.ContentResolver
import android.net.Uri
import com.verbanode.mobile.network.VerbaNodeApi
import org.json.JSONObject

internal data class DownloadPayload(val bytes: ByteArray, val filename: String, val mimeType: String)

internal class ContentTransferCoordinator(
    private val resolver: ContentResolver,
    private val sessionProvider: () -> Pair<VerbaNodeApi, String>,
) {
    fun exportAgent(agentId: Int): DownloadPayload {
        val (api, token) = sessionProvider()
        return DownloadPayload(
            bytes = api.agentBackup(token, agentId),
            filename = "verbanode-agent-$agentId.json",
            mimeType = "application/json",
        )
    }

    fun exportDiagnostics(): DownloadPayload {
        val (api, token) = sessionProvider()
        return DownloadPayload(
            bytes = api.diagnosticsExport(token),
            filename = "verbanode-diagnostics.zip",
            mimeType = "application/zip",
        )
    }

    fun exportBackup(): DownloadPayload {
        val (api, token) = sessionProvider()
        return DownloadPayload(
            bytes = api.downloadBackup(token),
            filename = "verbanode-backup.zip",
            mimeType = "application/zip",
        )
    }

    fun uploadKnowledgeDocument(
        libraryId: Int,
        uri: Uri,
        filename: String,
        mimeType: String,
        contentLength: Long?,
    ): JSONObject {
        val (api, token) = sessionProvider()
        return api.uploadKnowledgeDocument(token, libraryId, filename, mimeType, contentLength) {
            open(uri, "knowledge document")
        }
    }

    fun uploadAudio(
        uri: Uri,
        filename: String,
        mimeType: String,
        contentLength: Long?,
    ): JSONObject {
        val (api, token) = sessionProvider()
        return api.uploadAudio(token, filename, mimeType, contentLength) {
            open(uri, "audio file")
        }
    }

    fun restoreBackup(uri: Uri, filename: String, contentLength: Long?): JSONObject {
        val (api, token) = sessionProvider()
        return api.restoreBackup(token, filename, contentLength) {
            open(uri, "backup")
        }
    }

    private fun open(uri: Uri, label: String) =
        resolver.openInputStream(uri) ?: error("Could not open $label")
}
