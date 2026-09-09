package com.verbanode.mobile.chat

data class ChatComposerState(
    val draft: String = "",
    val pendingText: String? = null,
    val retryText: String? = null,
)

fun beginChatSend(state: ChatComposerState): ChatComposerState {
    if (state.pendingText != null) return state
    val text = state.draft.trim()
    if (text.isBlank()) return state
    return state.copy(draft = "", pendingText = text, retryText = null)
}

fun completeChatSend(state: ChatComposerState): ChatComposerState =
    state.copy(pendingText = null, retryText = null)

fun failChatSend(state: ChatComposerState): ChatComposerState {
    val failed = state.pendingText ?: return state
    return state.copy(draft = failed, pendingText = null, retryText = failed)
}
