package com.verbanode.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.verbanode.mobile.ui.VerbaNodeApp
import com.verbanode.mobile.ui.VerbaNodeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private var startDiscoveryAfterPermission = false
    private var pendingPairingLink: String? = null
    private var pendingDocumentBytes: ByteArray? = null

    private data class SelectedDocument(
        val uri: Uri,
        val filename: String,
        val mimeType: String,
        val contentLength: Long?,
    )

    private fun selectedDocument(uri: Uri, fallbackName: String, fallbackMime: String): SelectedDocument {
        var filename: String? = null
        var size: Long? = null
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { index ->
                    if (!cursor.isNull(index)) filename = cursor.getString(index)
                }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { index ->
                    if (!cursor.isNull(index)) size = cursor.getLong(index)
                }
            }
        }
        return SelectedDocument(
            uri = uri,
            filename = filename?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment ?: fallbackName,
            mimeType = contentResolver.getType(uri)?.takeIf { it.isNotBlank() } ?: fallbackMime,
            contentLength = size?.takeIf { it >= 0L },
        )
    }
    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = pendingDocumentBytes
        pendingDocumentBytes = null
        if (uri != null && bytes != null) {
            runCatching { contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
                .onSuccess { Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(this, it.message ?: "Could not save file", Toast.LENGTH_LONG).show() }
        }
    }
    private val openAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { selectedDocument(uri, "audio", "application/octet-stream") }
                .onSuccess { file -> viewModel.uploadAudio(file.uri, file.filename, file.mimeType, file.contentLength) }
                .onFailure { viewModel.reportError(it.message ?: "Could not read audio file metadata") }
        }
    }

    private val openKnowledge = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { selectedDocument(uri, "knowledge-document", "application/octet-stream") }
                .onSuccess { file -> viewModel.uploadKnowledgeDocument(file.uri, file.filename, file.mimeType, file.contentLength) }
                .onFailure { viewModel.reportError(it.message ?: "Could not read knowledge document metadata") }
        }
    }

    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { selectedDocument(uri, "verbanode-backup.zip", "application/zip") }
                .onSuccess { file -> viewModel.restoreBackup(file.uri, file.filename, file.contentLength) }
                .onFailure { viewModel.reportError(it.message ?: "Could not read backup metadata") }
        }
    }

    private val localNetworkPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted || Build.VERSION.SDK_INT < 37) {
            if (startDiscoveryAfterPermission) viewModel.startDiscovery()
            pendingPairingLink?.let(viewModel::pairFromQr)
        }
        startDiscoveryAfterPermission = false
        pendingPairingLink = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyImmersiveSystemBars()
        setContent {
            VerbaNodeTheme {
                VerbaNodeApp(viewModel, this)
            }
        }
        handleIntent(intent)
        if (!(intent?.data?.scheme == "verbanode" && intent.data?.host == "pair")) {
            ensureLocalNetworkPermission(false)
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveSystemBars()
    }

    private fun applyImmersiveSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    fun saveDocument(bytes: ByteArray, filename: String, mimeType: String = "application/octet-stream") {
        pendingDocumentBytes = bytes
        createDocument.launch(filename)
    }

    fun chooseAudioForUpload() {
        openAudio.launch(arrayOf("audio/*", "audio/mpeg", "video/mpeg", "application/ogg", "application/octet-stream"))
    }

    fun chooseKnowledgeForUpload() {
        openKnowledge.launch(arrayOf("*/*"))
    }

    fun chooseBackupForRestore() {
        openBackup.launch(arrayOf("application/zip", "application/octet-stream"))
    }

    fun ensureLocalNetworkAndDiscover() {
        ensureLocalNetworkPermission(true)
    }

    fun pairFromQrWithPermission(raw: String) {
        if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            pendingPairingLink = raw
            startDiscoveryAfterPermission = false
            localNetworkPermission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            viewModel.pairFromQr(raw)
        }
    }

    private fun ensureLocalNetworkPermission(startDiscovery: Boolean) {
        if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            startDiscoveryAfterPermission = startDiscovery
            localNetworkPermission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else if (startDiscovery) {
            viewModel.startDiscovery()
        }
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "verbanode" && uri.host == "pair") {
            pairFromQrWithPermission(uri.toString())
        }
    }
}
