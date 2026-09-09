package com.verbanode.mobile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImmersiveRootInsetsSourceTest {
    private fun source(relative: String): String {
        val candidates = listOf(File(relative), File("app/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Could not locate $relative from ${File(".").absolutePath}")
    }

    @Test
    fun immersiveRootDoesNotReserveOldStatusBarBand() {
        val app = source("src/main/kotlin/com/verbanode/mobile/ui/VerbaNodeApp.kt")
        assertFalse(app.contains("safeDrawingPadding"))
        assertFalse(app.contains(".safeDrawingPadding()"))

        val dashboard = source("src/main/kotlin/com/verbanode/mobile/ui/DashboardShell.kt")
        val management = source("src/main/kotlin/com/verbanode/mobile/ui/ManagementScreens.kt")
        assertTrue(dashboard.contains("contentWindowInsets = WindowInsets(0, 0, 0, 0)"))
        assertTrue(management.contains("contentWindowInsets = WindowInsets(0, 0, 0, 0)"))
        assertTrue(management.contains("windowInsets = WindowInsets(0, 0, 0, 0)"))
    }

    @Test
    fun transientSwipeRevealRemainsEnabled() {
        val activity = source("src/main/kotlin/com/verbanode/mobile/MainActivity.kt")
        assertTrue(activity.contains("BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE"))
        assertTrue(activity.contains("SYSTEM_UI_FLAG_IMMERSIVE_STICKY"))
    }
}
