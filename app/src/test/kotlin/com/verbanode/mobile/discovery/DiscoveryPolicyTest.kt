package com.verbanode.mobile.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryPolicyTest {
    @Test
    fun concurrentSetKeepsSetSemanticsOnApi23CompatibleBacking() {
        val values = newConcurrentMutableSet<String>()
        assertTrue(values.add("probe"))
        assertFalse(values.add("probe"))
        assertTrue(values.remove("probe"))
        assertTrue(values.isEmpty())
    }

    @Test
    fun activeDiscoveryFieldsRequireVerbaNodeAndProtocolV1() {
        val candidate = parseActiveDiscoveryFields(
            mapOf(
                "product" to "VerbaNode",
                "discovery_protocol" to "1",
                "instance_id" to "core-1",
                "instance_name" to "Studio PC",
                "version" to "0.12.5",
                "https_port" to "8002",
                "api_version" to "1",
                "websocket_protocol_version" to "1",
                "spki_sha256" to "a".repeat(64),
            ),
            host = "192.168.1.20",
            source = DiscoverySource.UDP,
        )
        assertEquals("https://192.168.1.20:8002", candidate.baseUrl)
        assertEquals("core-1", candidate.instanceId)
        assertEquals(DiscoverySource.UDP, candidate.source)
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeDiscoveryRejectsWrongProduct() {
        parseActiveDiscoveryFields(
            mapOf("product" to "Other", "discovery_protocol" to "1", "https_port" to "8002"),
            host = "192.168.1.20",
            source = DiscoverySource.UDP,
        )
    }

    @Test
    fun identityPrefersInstanceThenSpkiThenUrl() {
        assertEquals("instance:abc", discoveryIdentityKey("abc", "b".repeat(64), "https://1.2.3.4:8002"))
        assertEquals("spki:${"b".repeat(64)}", discoveryIdentityKey(null, "B".repeat(64), "https://1.2.3.4:8002"))
        assertEquals("url:https://1.2.3.4:8002", discoveryIdentityKey(null, null, "https://1.2.3.4:8002/"))
    }

    @Test
    fun subnetFallbackIsBoundedToPhonesLocal24() {
        val hosts = subnetFallbackHosts("192.168.50.42", prefixLength = 16)
        assertEquals(253, hosts.size)
        assertFalse(hosts.contains("192.168.50.42"))
        assertFalse(hosts.contains("192.168.50.0"))
        assertFalse(hosts.contains("192.168.50.255"))
        assertTrue(hosts.contains("192.168.50.1"))
        assertTrue(hosts.contains("192.168.50.254"))
        assertFalse(hosts.contains("192.168.49.1"))
    }

    @Test
    fun narrowSubnetsStayInsideTheirPrefix() {
        val hosts = subnetFallbackHosts("10.0.0.10", prefixLength = 29)
        assertEquals(listOf("10.0.0.9", "10.0.0.11", "10.0.0.12", "10.0.0.13", "10.0.0.14"), hosts)
    }

    @Test
    fun resolveRetriesAreBounded() {
        assertEquals(150L, resolveRetryDelayMs(0))
        assertEquals(350L, resolveRetryDelayMs(1))
        assertEquals(700L, resolveRetryDelayMs(2))
        assertEquals(null, resolveRetryDelayMs(3))
    }
}
