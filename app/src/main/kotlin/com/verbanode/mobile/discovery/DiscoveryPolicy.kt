package com.verbanode.mobile.discovery

import java.net.URI
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap


internal fun <T> newConcurrentMutableSet(): MutableSet<T> =
    Collections.newSetFromMap(ConcurrentHashMap<T, Boolean>())

fun discoveryIdentityKey(instanceId: String?, spkiSha256: String?, baseUrl: String): String {
    val instance = instanceId?.trim()?.takeIf { it.isNotEmpty() }
    if (instance != null) return "instance:$instance"
    val spki = spkiSha256?.trim()?.lowercase()?.takeIf { it.length == 64 }
    if (spki != null) return "spki:$spki"
    return "url:${baseUrl.trim().trimEnd('/').lowercase()}"
}

fun resolveRetryDelayMs(attempt: Int): Long? = when (attempt) {
    0 -> 150L
    1 -> 350L
    2 -> 700L
    else -> null
}

fun subnetFallbackHosts(localIpv4: String, prefixLength: Int): List<String> {
    val local = ipv4ToLong(localIpv4)
    require(prefixLength in 0..32) { "Invalid IPv4 prefix length" }
    // Home LANs are overwhelmingly /24. On broader enterprise networks, cap
    // active probing to the phone's local /24 to keep the scan bounded.
    val effectivePrefix = maxOf(prefixLength, 24)
    if (effectivePrefix >= 31) return emptyList()
    val mask = if (effectivePrefix == 0) 0L else (0xffffffffL shl (32 - effectivePrefix)) and 0xffffffffL
    val network = local and mask
    val broadcast = network or (mask.inv() and 0xffffffffL)
    return buildList {
        var value = network + 1
        while (value < broadcast) {
            if (value != local) add(longToIpv4(value))
            value += 1
        }
    }
}

fun directedBroadcastAddress(localIpv4: String, prefixLength: Int): String {
    val local = ipv4ToLong(localIpv4)
    require(prefixLength in 0..32) { "Invalid IPv4 prefix length" }
    val mask = if (prefixLength == 0) 0L else (0xffffffffL shl (32 - prefixLength)) and 0xffffffffL
    return longToIpv4(local or (mask.inv() and 0xffffffffL))
}

fun rememberedPort(baseUrl: String): Int? = runCatching {
    val uri = URI(baseUrl)
    when {
        uri.port in 1..65535 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> null
    }
}.getOrNull()

private fun ipv4ToLong(value: String): Long {
    val parts = value.split('.')
    require(parts.size == 4) { "Invalid IPv4 address" }
    return parts.fold(0L) { acc, part ->
        val octet = part.toIntOrNull() ?: throw IllegalArgumentException("Invalid IPv4 address")
        require(octet in 0..255) { "Invalid IPv4 address" }
        (acc shl 8) or octet.toLong()
    }
}

private fun longToIpv4(value: Long): String = listOf(
    (value shr 24) and 0xff,
    (value shr 16) and 0xff,
    (value shr 8) and 0xff,
    value and 0xff,
).joinToString(".")
