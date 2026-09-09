package com.verbanode.mobile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ImmersiveModeSourceTest {
    @Test
    fun appUsesTransientSwipeRevealForImmersiveSystemBars() {
        val relative = "src/main/kotlin/com/verbanode/mobile/MainActivity.kt"
        val sourceFile = sequenceOf(
            File(relative),
            File("app/$relative"),
            File("../app/$relative"),
        ).firstOrNull(File::isFile)
            ?: error("Could not locate MainActivity.kt from ${System.getProperty("user.dir")}")
        val source = sourceFile.readText()
        assertTrue(source.contains("WindowInsets.Type.systemBars()"))
        assertTrue(source.contains("BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE"))
        assertTrue(source.contains("SYSTEM_UI_FLAG_IMMERSIVE_STICKY"))
        assertTrue(source.contains("onWindowFocusChanged"))
    }
}
