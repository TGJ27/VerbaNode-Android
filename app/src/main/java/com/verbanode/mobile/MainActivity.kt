package com.verbanode.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
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
            runCatching {
                val filename = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment ?: "audio.wav"
                val mime = contentResolver.getType(uri) ?: if (filename.lowercase().endsWith(".mp3")) "audio/mpeg" else "audio/wav"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not open audio file")
                Triple(bytes, filename, mime)
            }.onSuccess { (bytes, filename, mime) -> viewModel.uploadAudio(bytes, filename, mime) }
                .onFailure { viewModel.reportError(it.message ?: "Could not read audio file") }
        }
    }

    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not open backup") }
                .onSuccess { bytes -> viewModel.restoreBackup(bytes, uri.lastPathSegment ?: "verbanode-backup.zip") }
                .onFailure { viewModel.reportError(it.message ?: "Could not read backup") }
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
        openAudio.launch(arrayOf("audio/mpeg", "audio/wav", "audio/x-wav"))
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
