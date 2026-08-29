package com.verbanode.mobile.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class PttRecorder {
    companion object {
        const val SAMPLE_RATE = 16000
    }

    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private val recording = AtomicBoolean(false)
    private var pcm = ByteArrayOutputStream()

    @Synchronized
    fun start() {
        if (recording.get()) return
        val minimum = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        require(minimum > 0) { "Android could not initialize the microphone" }
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minimum.coerceAtLeast(4096) * 2,
        )
        require(audioRecord.state == AudioRecord.STATE_INITIALIZED) { "Microphone initialization failed" }
        pcm = ByteArrayOutputStream()
        recorder = audioRecord
        recording.set(true)
        audioRecord.startRecording()
        worker = thread(name = "VerbaNode-PTT", isDaemon = true) {
            val buffer = ByteArray(minimum.coerceAtLeast(4096))
            while (recording.get()) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) synchronized(this) { pcm.write(buffer, 0, read) }
            }
        }
    }

    @Synchronized
    fun stop(): ByteArray {
        if (!recording.getAndSet(false)) return ByteArray(0)
        runCatching { recorder?.stop() }
        worker?.join(1000)
        worker = null
        recorder?.release()
        recorder = null
        val data = pcm.toByteArray()
        pcm.reset()
        return encodeWav(data)
    }

    @Synchronized
    fun cancel() {
        if (recording.getAndSet(false)) runCatching { recorder?.stop() }
        worker?.join(500)
        worker = null
        recorder?.release()
        recorder = null
        pcm.reset()
    }

    fun isRecording(): Boolean = recording.get()

    internal fun encodeWav(pcm16: ByteArray): ByteArray {
        val output = ByteArray(44 + pcm16.size)
        val buffer = ByteBuffer.wrap(output).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + pcm16.size)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(1)
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcm16.size)
        buffer.put(pcm16)
        return output
    }
}
