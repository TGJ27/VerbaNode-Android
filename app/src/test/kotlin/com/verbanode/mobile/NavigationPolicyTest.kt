package com.verbanode.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {
    @Test
    fun primaryBottomTabsStayInOneStableOrder() {
        assertEquals(
            listOf(AppScreen.HOME, AppScreen.CHAT, AppScreen.SCRIPTS, AppScreen.AUDIO, AppScreen.MORE),
            NavigationPolicy.primaryDestinations,
        )
    }

    @Test
    fun moreChildrenKeepMoreSelectedWhileFocusedScreensHaveNoBottomSelection() {
        listOf(
            AppScreen.AGENTS,
            AppScreen.KNOWLEDGE,
            AppScreen.PLUGINS,
            AppScreen.SETTINGS,
            AppScreen.DEVICES,
            AppScreen.DIAGNOSTICS,
            AppScreen.DATA,
            AppScreen.STATUS,
        ).forEach { screen ->
            assertEquals(AppScreen.MORE, NavigationPolicy.bottomSelectionFor(screen))
        }

        assertNull(NavigationPolicy.bottomSelectionFor(AppScreen.TYPE_TO_TALK))
        assertNull(NavigationPolicy.bottomSelectionFor(AppScreen.PUSH_TO_TALK))
    }

    @Test
    fun systemBackReturnsPostSetupScreensToHomeButLeavesOnboardingAndHomeToAndroid() {
        listOf(
            AppScreen.CHAT,
            AppScreen.SCRIPTS,
            AppScreen.AUDIO,
            AppScreen.MORE,
            AppScreen.AGENTS,
            AppScreen.KNOWLEDGE,
            AppScreen.PLUGINS,
            AppScreen.SETTINGS,
            AppScreen.DEVICES,
            AppScreen.DIAGNOSTICS,
            AppScreen.DATA,
            AppScreen.STATUS,
            AppScreen.TYPE_TO_TALK,
            AppScreen.PUSH_TO_TALK,
        ).forEach { screen -> assertTrue(NavigationPolicy.systemBackReturnsHome(screen)) }

        listOf(AppScreen.SERVERS, AppScreen.TRUST, AppScreen.LOGIN, AppScreen.HOME).forEach { screen ->
            assertFalse(NavigationPolicy.systemBackReturnsHome(screen))
        }
    }

    @Test
    fun losingAControllerSessionKeepsConfiguredUsersAtHome() {
        assertEquals(AppScreen.HOME, NavigationPolicy.sessionLossDestination(hasConfiguredServer = true))
        assertEquals(AppScreen.SERVERS, NavigationPolicy.sessionLossDestination(hasConfiguredServer = false))
    }
}
