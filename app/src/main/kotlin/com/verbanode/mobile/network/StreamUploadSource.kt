package com.verbanode.mobile.network

import java.io.InputStream
import java.io.OutputStream

internal class StreamUploadSource(
    private val contentLength: Long?,
    private val openStream: () -> InputStream,
) {
    fun lengthOrUnknown(): Long = contentLength?.takeIf { it >= 0L } ?: -1L

    fun writeTo(output: OutputStream) {
        openStream().use { input -> input.copyTo(output) }
    }
}
