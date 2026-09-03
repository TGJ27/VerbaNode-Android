package com.verbanode.mobile.network

import java.net.URI

private val SPKI_SHA256_HEX = Regex("^[0-9a-fA-F]{64}$")

internal fun normalizeSpkiSha256(raw: String): String {
    val value = raw.trim()
    require(SPKI_SHA256_HEX.matches(value)) { "Invalid VerbaNode certificate identity" }
    return value.lowercase()
}

internal fun normalizeTlsBaseUrl(raw: String): String {
    val trimmed = raw.trim().trimEnd('/')
    require(trimmed.isNotBlank()) { "VerbaNode server address is required" }

    val schemeMatch = Regex("^[A-Za-z][A-Za-z0-9+.-]*://").find(trimmed)
    val value = when {
        schemeMatch == null -> "https://$trimmed"
        trimmed.startsWith("https://", ignoreCase = true) -> "https://${trimmed.substring(schemeMatch.range.last + 1)}"
        else -> throw IllegalArgumentException("VerbaNode requires an HTTPS server address")
    }

    val uri = runCatching { URI(value) }
        .getOrElse { throw IllegalArgumentException("Invalid VerbaNode server address", it) }
    require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
        "Invalid VerbaNode server address"
    }
    require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
        "Invalid VerbaNode server address"
    }
    require(uri.path.isNullOrEmpty() || uri.path == "/") {
        "VerbaNode server address must not include a path"
    }
    return value.trimEnd('/')
}
