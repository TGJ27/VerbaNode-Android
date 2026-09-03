package com.verbanode.mobile.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TlsIdentityTest {
    @Test
    fun bareLanAddressGetsHttpsScheme() {
        assertEquals("https://192.168.1.20:8765", normalizeTlsBaseUrl("192.168.1.20:8765/"))
    }

    @Test
    fun explicitHttpsAddressIsPreservedWithoutTrailingSlash() {
        assertEquals("https://verbanode.local:8765", normalizeTlsBaseUrl("https://verbanode.local:8765///"))
    }

    @Test
    fun explicitHttpAddressIsRejectedInsteadOfBeingRewritten() {
        assertThrows(IllegalArgumentException::class.java) {
            normalizeTlsBaseUrl("http://verbanode.local:8765")
        }
    }

    @Test
    fun spkiIdentityMustBeSha256HexAndNormalizesCase() {
        assertEquals("ab".repeat(32), normalizeSpkiSha256("AB".repeat(32)))
        assertThrows(IllegalArgumentException::class.java) { normalizeSpkiSha256("x".repeat(64)) }
        assertThrows(IllegalArgumentException::class.java) { normalizeSpkiSha256("ab") }
    }
}
