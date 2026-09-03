package com.verbanode.mobile.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PttRecorderTest {
    @Test
    fun wavEncoderWritesPcm16MonoHeader() {
        val pcm = byteArrayOf(1, 2, 3, 4)
        val wav = PttRecorder().encodeWav(pcm)

        assertEquals(48, wav.size)
        assertEquals("RIFF", String(wav.copyOfRange(0, 4), Charsets.US_ASCII))
        assertEquals("WAVE", String(wav.copyOfRange(8, 12), Charsets.US_ASCII))
        assertEquals("fmt ", String(wav.copyOfRange(12, 16), Charsets.US_ASCII))
        assertEquals("data", String(wav.copyOfRange(36, 40), Charsets.US_ASCII))
        assertTrue(wav.copyOfRange(44, 48).contentEquals(pcm))
    }

    @Test
    fun wavEncoderWritesExpectedAudioFormatFields() {
        val wav = PttRecorder().encodeWav(byteArrayOf(1, 2, 3, 4))
        val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(40, buffer.getInt(4))
        assertEquals(1, buffer.getShort(20).toInt())
        assertEquals(1, buffer.getShort(22).toInt())
        assertEquals(PttRecorder.SAMPLE_RATE, buffer.getInt(24))
        assertEquals(PttRecorder.SAMPLE_RATE * 2, buffer.getInt(28))
        assertEquals(2, buffer.getShort(32).toInt())
        assertEquals(16, buffer.getShort(34).toInt())
        assertEquals(4, buffer.getInt(40))
    }

    @Test
    fun emptyPcmStillProducesValidEmptyWav() {
        val wav = PttRecorder().encodeWav(ByteArray(0))
        assertEquals(44, wav.size)
        val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(36, buffer.getInt(4))
        assertEquals(0, buffer.getInt(40))
    }
}
