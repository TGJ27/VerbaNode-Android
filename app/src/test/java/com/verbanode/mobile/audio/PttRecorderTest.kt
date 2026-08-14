package com.verbanode.mobile.audio

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
}
