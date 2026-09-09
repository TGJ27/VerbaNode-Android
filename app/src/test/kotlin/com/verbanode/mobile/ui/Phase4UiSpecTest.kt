package com.verbanode.mobile.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Phase4UiSpecTest {
    @Test
    fun settingsUsesOnlyCurrentProductSections() {
        assertEquals(listOf("Conversation", "Audio", "AI / Models", "Runtime"), Phase4UiSpec.settingsSections)
    }

    @Test
    fun diagnosticsKeepsCurrentTrustedInformationArchitecture() {
        assertEquals(
            listOf("Compatibility", "Connection & trust", "Core health", "Recent sanitized logs", "Diagnostics export"),
            Phase4UiSpec.diagnosticsSections,
        )
    }

    @Test
    fun globalPolishUsesSixteenDpHorizontalPaddingAndNoGlobalSafeDrawingInset() {
        assertEquals(16, Phase4UiSpec.horizontalPaddingDp)
        assertEquals(48, Phase4UiSpec.minimumTouchTargetDp)
        assertEquals(true, Phase4UiSpec.keyboardAware)
        assertFalse(Phase4UiSpec.reserveSystemBarInset)
    }
}
