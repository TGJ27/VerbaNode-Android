package com.verbanode.mobile.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatComposerPolicyTest {
    @Test
    fun blankSendIsIgnored() {
        val state = ChatComposerState(draft = "   ")
        assertEquals(state, beginChatSend(state))
    }

    @Test
    fun beginSendMovesTrimmedDraftToPending() {
        val result = beginChatSend(ChatComposerState(draft = "  hello  "))
        assertEquals("", result.draft)
        assertEquals("hello", result.pendingText)
        assertNull(result.retryText)
    }

    @Test
    fun successClearsPendingAndRetry() {
        val result = completeChatSend(ChatComposerState(draft = "", pendingText = "hello", retryText = "old"))
        assertEquals(ChatComposerState(), result)
    }

    @Test
    fun failureRestoresDraftAndRetryText() {
        val result = failChatSend(ChatComposerState(draft = "", pendingText = "hello"))
        assertEquals("hello", result.draft)
        assertNull(result.pendingText)
        assertEquals("hello", result.retryText)
    }
}
