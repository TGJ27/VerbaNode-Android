package com.verbanode.mobile.network

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class StreamUploadSourceTest {
    @Test
    fun streamIsOpenedOnlyWhenTransferStarts() {
        var opens = 0
        val source = StreamUploadSource(5L) {
            opens += 1
            ByteArrayInputStream("hello".toByteArray())
        }

        assertEquals(0, opens)
        assertEquals(5L, source.lengthOrUnknown())

        val output = ByteArrayOutputStream()
        source.writeTo(output)

        assertEquals(1, opens)
        assertArrayEquals("hello".toByteArray(), output.toByteArray())
    }

    @Test
    fun missingOrNegativeLengthIsReportedAsUnknown() {
        assertEquals(-1L, StreamUploadSource(null) { ByteArrayInputStream(byteArrayOf()) }.lengthOrUnknown())
        assertEquals(-1L, StreamUploadSource(-1L) { ByteArrayInputStream(byteArrayOf()) }.lengthOrUnknown())
    }
}
