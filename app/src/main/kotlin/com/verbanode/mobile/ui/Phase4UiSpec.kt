package com.verbanode.mobile.ui

internal object Phase4UiSpec {
    val settingsSections = listOf("Conversation", "Audio", "AI / Models", "Runtime")
    val diagnosticsSections = listOf(
        "Compatibility",
        "Connection & trust",
        "Core health",
        "Recent sanitized logs",
        "Diagnostics export",
    )
    const val horizontalPaddingDp = 16
    const val minimumTouchTargetDp = 48
    const val keyboardAware = true
    const val reserveSystemBarInset = false
}
