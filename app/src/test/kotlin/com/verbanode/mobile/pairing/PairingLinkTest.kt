package com.verbanode.mobile.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PairingLinkTest {
    private val secret = "0123456789abcdefghij"
    private val spki = "ab".repeat(32)
    private val fingerprint = "cd".repeat(32)

    @Test
    fun parsesCanonicalPairingLink() {
        val link = parsePairingLink(
            "verbanode://pair?server=https%3A%2F%2F192.168.1.20%3A8765%2F&pairing_id=pair-1&secret=$secret&spki=${spki.uppercase()}&fingerprint=${fingerprint.uppercase()}",
        )
        assertEquals("https://192.168.1.20:8765", link.serverUrl)
        assertEquals("pair-1", link.pairingId)
        assertEquals(secret, link.secret)
        assertEquals(spki, link.spkiSha256)
        assertEquals(fingerprint, link.fingerprintSha256)
    }

    @Test
    fun fingerprintIsOptional() {
        val link = parsePairingLink(
            "verbanode://pair?server=https%3A%2F%2Fverbanode.local%3A8765&pairing_id=pair-1&secret=$secret&spki=$spki",
        )
        assertNull(link.fingerprintSha256)
    }

    @Test
    fun rejectsNonHttpsServer() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePairingLink(
                "verbanode://pair?server=http%3A%2F%2Fverbanode.local%3A8765&pairing_id=pair-1&secret=$secret&spki=$spki",
            )
        }
    }

    @Test
    fun rejectsInvalidServerIdentity() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePairingLink(
                "verbanode://pair?server=https%3A%2F%2Fverbanode.local%3A8765&pairing_id=pair-1&secret=$secret&spki=${"x".repeat(64)}",
            )
        }
    }

    @Test
    fun rejectsInvalidOptionalFingerprint() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePairingLink(
                "verbanode://pair?server=https%3A%2F%2Fverbanode.local%3A8765&pairing_id=pair-1&secret=$secret&spki=$spki&fingerprint=${"z".repeat(64)}",
            )
        }
    }
}
