package com.verbanode.mobile.discovery

const val ACTIVE_DISCOVERY_PROTOCOL_VERSION = 1
const val ACTIVE_DISCOVERY_REQUEST = "VERBANODE_DISCOVER/1"
const val DEFAULT_VERBANODE_PORT = 8002

enum class DiscoveryStage(val label: String) {
    IDLE("Ready to scan"),
    SAVED("Checking saved VerbaNodes"),
    LAN("Listening for VerbaNode on this Wi-Fi"),
    SUBNET("Checking reachable devices on this Wi-Fi"),
    COMPLETE("Scan complete"),
}

enum class DiscoverySource(val label: String) {
    SAVED("Saved"),
    MDNS("mDNS"),
    UDP("LAN broadcast"),
    SUBNET("HTTPS scan"),
}

data class DiscoveryCandidate(
    val serviceName: String,
    val host: String,
    val httpsPort: Int,
    val instanceId: String? = null,
    val version: String? = null,
    val apiVersion: Int? = null,
    val wsVersion: Int? = null,
    val spkiSha256: String? = null,
    val source: DiscoverySource,
) {
    val baseUrl: String get() {
        val urlHost = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
        return "https://$urlHost:$httpsPort"
    }
}

private val SHA256_HEX = Regex("^[0-9a-fA-F]{64}$")

fun parseActiveDiscoveryFields(
    fields: Map<String, String>,
    host: String,
    source: DiscoverySource = DiscoverySource.UDP,
): DiscoveryCandidate {
    require(fields["product"] == "VerbaNode") { "Discovery response is not VerbaNode" }
    require(fields["discovery_protocol"]?.toIntOrNull() == ACTIVE_DISCOVERY_PROTOCOL_VERSION) {
        "Unsupported VerbaNode discovery protocol"
    }
    val port = fields["https_port"]?.toIntOrNull()
    require(port != null && port in 1..65535) { "Discovery response has an invalid HTTPS port" }
    val spki = fields["spki_sha256"]?.lowercase()?.takeIf(SHA256_HEX::matches)
        ?: throw IllegalArgumentException("Discovery response is missing a valid TLS identity")
    val instanceId = fields["instance_id"]?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("Discovery response is missing instance identity")
    val apiVersion = fields["api_version"]?.toIntOrNull()
        ?: throw IllegalArgumentException("Discovery response is missing API version")
    val wsVersion = fields["websocket_protocol_version"]?.toIntOrNull()
        ?: throw IllegalArgumentException("Discovery response is missing WebSocket version")
    require(apiVersion > 0 && wsVersion > 0) { "Discovery response has invalid protocol versions" }
    val name = fields["instance_name"]?.trim()?.takeIf { it.isNotEmpty() } ?: "VerbaNode"
    return DiscoveryCandidate(
        serviceName = name,
        host = host,
        httpsPort = port,
        instanceId = instanceId,
        version = fields["version"]?.trim()?.takeIf { it.isNotEmpty() },
        apiVersion = apiVersion,
        wsVersion = wsVersion,
        spkiSha256 = spki,
        source = source,
    )
}
