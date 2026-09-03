package com.verbanode.mobile.pairing

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private val SHA256_HEX = Regex("^[0-9a-fA-F]{64}$")

data class PairingLink(
    val serverUrl: String,
    val pairingId: String,
    val secret: String,
    val fingerprintSha256: String?,
    val spkiSha256: String,
)

private fun decodeQueryPart(value: String): String =
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())

private fun pairingParameters(uri: URI): Map<String, String> = buildMap {
    uri.rawQuery.orEmpty().split('&').filter { it.isNotBlank() }.forEach { field ->
        val separator = field.indexOf('=')
        val rawKey = if (separator >= 0) field.substring(0, separator) else field
        val rawValue = if (separator >= 0) field.substring(separator + 1) else ""
        put(decodeQueryPart(rawKey), decodeQueryPart(rawValue))
    }
}

fun parsePairingLink(raw: String): PairingLink {
    val uri = runCatching { URI(raw.trim()) }
        .getOrElse { throw IllegalArgumentException("This QR code is not a valid VerbaNode pairing code", it) }
    require(uri.scheme.equals("verbanode", ignoreCase = true) && uri.host.equals("pair", ignoreCase = true)) {
        "This QR code is not a VerbaNode pairing code"
    }

    val params = pairingParameters(uri)
    val server = params["server"]?.trim()?.trimEnd('/').orEmpty()
    val serverUri = runCatching { URI(server) }.getOrNull()
    require(
        serverUri != null &&
            serverUri.scheme.equals("https", ignoreCase = true) &&
            !serverUri.host.isNullOrBlank() &&
            serverUri.userInfo == null &&
            serverUri.query == null &&
            serverUri.fragment == null &&
            (serverUri.path.isNullOrEmpty() || serverUri.path == "/"),
    ) { "Pairing code has an invalid server address" }

    val pairingId = params["pairing_id"].orEmpty()
    val secret = params["secret"].orEmpty()
    require(pairingId.isNotBlank() && secret.length >= 20) { "Pairing code is incomplete" }

    val spki = params["spki"]?.trim().orEmpty()
    require(SHA256_HEX.matches(spki)) { "Pairing code is missing the trusted server identity" }

    val fingerprint = params["fingerprint"]?.trim()?.takeIf { it.isNotBlank() }?.also {
        require(SHA256_HEX.matches(it)) { "Pairing code has an invalid certificate fingerprint" }
    }

    return PairingLink(
        serverUrl = server,
        pairingId = pairingId,
        secret = secret,
        fingerprintSha256 = fingerprint?.lowercase(),
        spkiSha256 = spki.lowercase(),
    )
}
