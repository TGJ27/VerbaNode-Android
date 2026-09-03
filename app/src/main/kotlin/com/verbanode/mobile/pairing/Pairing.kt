package com.verbanode.mobile.pairing

import android.app.Activity
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

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
