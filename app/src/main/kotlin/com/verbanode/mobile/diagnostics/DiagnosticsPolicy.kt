package com.verbanode.mobile.diagnostics

enum class DiagnosticHealth { GOOD, WARN, BAD, UNKNOWN }

fun diagnosticHealth(alive: Boolean?, error: String?): DiagnosticHealth = when {
    alive == false -> DiagnosticHealth.BAD
    !error.isNullOrBlank() && alive == null -> DiagnosticHealth.WARN
    !error.isNullOrBlank() -> DiagnosticHealth.BAD
    alive == true -> DiagnosticHealth.GOOD
    else -> DiagnosticHealth.UNKNOWN
}

fun compatibilityHealth(remoteFingerprint: String?, expectedFingerprint: String): DiagnosticHealth = when {
    remoteFingerprint.isNullOrBlank() -> DiagnosticHealth.UNKNOWN
    remoteFingerprint.equals(expectedFingerprint, ignoreCase = true) -> DiagnosticHealth.GOOD
    else -> DiagnosticHealth.BAD
}

fun abbreviateFingerprint(value: String?): String {
    val normalized = value.orEmpty().trim().lowercase()
    if (normalized.length < 24) return if (normalized.isBlank()) "unknown" else normalized
    return normalized.take(12) + "…" + normalized.takeLast(12)
}

private fun levelRank(level: String): Int = when (level.trim().uppercase()) {
    "CRITICAL", "FATAL" -> 50
    "ERROR" -> 40
    "WARNING", "WARN" -> 30
    "INFO" -> 20
    "DEBUG" -> 10
    else -> 0
}

fun includeDiagnosticLog(level: String, warningOrHigher: Boolean): Boolean =
    !warningOrHigher || levelRank(level) >= 30

private val diagnosticSecrets = listOf(
    Regex("(?i)(x-session-token\\s*[:=]\\s*)\\S+"),
    Regex("(?i)(authorization\\s*[:=]\\s*bearer\\s+)\\S+"),
    Regex("(?i)(pairing\\s+secret\\s*[:=]\\s*)\\S+"),
    Regex("(?i)(\\bpin\\s*[:=]\\s*)\\S+"),
    Regex("(?i)(device[_ -]?token\\s*[:=]\\s*)\\S+"),
)

fun safeDiagnosticMessage(value: String): String {
    var out = value
    diagnosticSecrets.forEach { regex -> out = regex.replace(out) { match -> match.groupValues[1] + "<redacted>" } }
    return out.take(4000)
}
