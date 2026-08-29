package com.verbanode.mobile.pairing

import android.app.Activity
import android.net.Uri
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode


data class PairingLink(
    val serverUrl: String,
    val pairingId: String,
    val secret: String,
    val fingerprintSha256: String?,
    val spkiSha256: String,
)

fun parsePairingLink(raw: String): PairingLink {
    val uri = Uri.parse(raw.trim())
    require(uri.scheme == "verbanode" && uri.host == "pair") { "This QR code is not a VerbaNode pairing code" }
    val server = uri.getQueryParameter("server")?.trim()?.removeSuffix("/").orEmpty()
    val pairingId = uri.getQueryParameter("pairing_id").orEmpty()
    val secret = uri.getQueryParameter("secret").orEmpty()
    val spki = uri.getQueryParameter("spki")?.lowercase().orEmpty()
    require(server.startsWith("https://")) { "Pairing code has an invalid server address" }
    require(pairingId.isNotBlank() && secret.length >= 20) { "Pairing code is incomplete" }
    require(spki.length == 64) { "Pairing code is missing the trusted server identity" }
    return PairingLink(
        serverUrl = server,
        pairingId = pairingId,
        secret = secret,
        fingerprintSha256 = uri.getQueryParameter("fingerprint")?.lowercase()?.takeIf { it.length == 64 },
        spkiSha256 = spki,
    )
}

fun scanVerbaNodeQr(activity: Activity, onResult: (String) -> Unit, onError: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAutoZoom()
        .build()
    GmsBarcodeScanning.getClient(activity, options)
        .startScan()
        .addOnSuccessListener { barcode ->
            val value = barcode.rawValue
            if (value.isNullOrBlank()) onError("The QR code did not contain pairing data") else onResult(value)
        }
        .addOnCanceledListener { onError("QR scan cancelled") }
        .addOnFailureListener { error -> onError(error.message ?: "QR scanner failed") }
}
