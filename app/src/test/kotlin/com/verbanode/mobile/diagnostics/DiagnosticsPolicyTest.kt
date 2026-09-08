package com.verbanode.mobile.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsPolicyTest {
    @Test
    fun healthMappingDistinguishesHealthyWarningAndFailure() {
        assertEquals(DiagnosticHealth.GOOD, diagnosticHealth(alive = true, error = null))
        assertEquals(DiagnosticHealth.BAD, diagnosticHealth(alive = false, error = "engine stopped"))
        assertEquals(DiagnosticHealth.WARN, diagnosticHealth(alive = null, error = "degraded"))
        assertEquals(DiagnosticHealth.UNKNOWN, diagnosticHealth(alive = null, error = null))
    }

    @Test
    fun compatibilityAndFingerprintFormattingAreSafe() {
        val fingerprint = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        assertEquals(DiagnosticHealth.GOOD, compatibilityHealth(fingerprint, fingerprint))
        assertEquals(DiagnosticHealth.BAD, compatibilityHealth("f".repeat(64), fingerprint))
        assertEquals("0123456789ab…456789abcdef", abbreviateFingerprint(fingerprint))
        assertEquals("unknown", abbreviateFingerprint(""))
    }

    @Test
    fun logFilteringKeepsWarningsAndErrorsWithoutSecrets() {
        assertFalse(includeDiagnosticLog("INFO", warningOrHigher = true))
        assertTrue(includeDiagnosticLog("WARNING", warningOrHigher = true))
        assertTrue(includeDiagnosticLog("ERROR", warningOrHigher = true))
        assertTrue(includeDiagnosticLog("INFO", warningOrHigher = false))
        assertEquals("X-Session-Token: <redacted>", safeDiagnosticMessage("X-Session-Token: abc123"))
        assertEquals("pairing secret=<redacted>", safeDiagnosticMessage("pairing secret=hunter2"))
    }
}
