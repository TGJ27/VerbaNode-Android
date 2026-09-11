package com.verbanode.mobile

internal object NavigationPolicy {
    val primaryDestinations: List<AppScreen> = listOf(
        AppScreen.HOME,
        AppScreen.CHAT,
        AppScreen.SCRIPTS,
        AppScreen.AUDIO,
        AppScreen.MORE,
    )

    fun bottomSelectionFor(screen: AppScreen): AppScreen? = when (screen) {
        AppScreen.HOME,
        AppScreen.CHAT,
        AppScreen.SCRIPTS,
        AppScreen.AUDIO,
        AppScreen.MORE -> screen

        AppScreen.AGENTS,
        AppScreen.KNOWLEDGE,
        AppScreen.PLUGINS,
        AppScreen.SETTINGS,
        AppScreen.DEVICES,
        AppScreen.DIAGNOSTICS,
        AppScreen.DATA,
        AppScreen.STATUS -> AppScreen.MORE

        AppScreen.SERVERS,
        AppScreen.TRUST,
        AppScreen.LOGIN,
        AppScreen.TYPE_TO_TALK,
        AppScreen.PUSH_TO_TALK -> null
    }

    fun systemBackReturnsHome(screen: AppScreen): Boolean = when (screen) {
        AppScreen.SERVERS,
        AppScreen.TRUST,
        AppScreen.LOGIN,
        AppScreen.HOME -> false

        else -> true
    }

    fun sessionLossDestination(hasConfiguredServer: Boolean): AppScreen =
        if (hasConfiguredServer) AppScreen.HOME else AppScreen.SERVERS
}
