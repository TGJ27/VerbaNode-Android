package com.verbanode.mobile

import com.verbanode.mobile.network.parseAudioLibraryItems
import com.verbanode.mobile.network.parseScriptItems
import com.verbanode.mobile.network.parseScriptQueueItems
import com.verbanode.mobile.network.parseTypeToTalkItems
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3ModelsTest {
    @Test
    fun directSpeechQueueUsesTypedItems() {
        val items = parseTypeToTalkItems(
            JSONArray("""[{"id":7,"text":"hello","position":2,"status":"playing","created_at":"now"}]""")
        )
        assertEquals(1, items.size)
        assertEquals(7, items.single().id)
        assertEquals("hello", items.single().text)
        assertEquals("playing", items.single().status)
    }

    @Test
    fun audioLibraryIgnoresNamelessEntriesAndKeepsNullableDuration() {
        val items = parseAudioLibraryItems(
            JSONArray(
                """[
                    {"name":"clip (2).mp3","size_bytes":4096,"duration_seconds":1.25,"playing":true},
                    {"name":"broken.mp3","size_bytes":4,"duration_seconds":null},
                    {"name":""}
                ]"""
            )
        )
        assertEquals(2, items.size)
        assertEquals("clip (2).mp3", items[0].name)
        assertEquals(1.25, items[0].durationSeconds!!, 0.0001)
        assertTrue(items[0].playing)
        assertNull(items[1].durationSeconds)
        assertFalse(items[1].playing)
    }

    @Test
    fun scriptsAndQueueUseTypedIdentityForReorderAndEditFlows() {
        val scripts = parseScriptItems(
            JSONArray("""[{"id":3,"title":"Welcome","text":"Hello","enabled":true,"language":"en","tts_mode":"edge"}]""")
        )
        val queue = parseScriptQueueItems(
            JSONArray("""[{"id":11,"script_id":3,"position":0,"status":"waiting","pause_after_seconds":2.5,"title":"Welcome"}]""")
        )
        assertEquals(3, scripts.single().id)
        assertEquals("edge", scripts.single().ttsMode)
        assertEquals(11, queue.single().id)
        assertEquals(2.5, queue.single().pauseAfterSeconds, 0.0001)
    }

    @Test
    fun statusFormattingIsIndependentOfViewModel() {
        assertEquals("Listening", pipelineStatusLabel("idle", "conversation"))
        assertEquals("Generating", pipelineStatusLabel("thinking", "idle"))
        assertEquals("Recording", modeStatusLabel("browser_ptt"))
    }
}
