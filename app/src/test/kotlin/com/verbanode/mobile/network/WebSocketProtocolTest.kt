package com.verbanode.mobile.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.json.JSONObject
import org.junit.Test

class WebSocketProtocolTest {
    private fun assertProtocolError(block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertNotNull("Expected protocol failure", error)
        assertEquals("ApiProtocolException", error!!.javaClass.simpleName)
    }

    @Test
    fun validProtocolEventParsesTypeAndObjectData() {
        val event = parseWebSocketEvent("""{"protocol":1,"type":"mode_changed","data":{"mode":"conversation"}}""")
        assertEquals("mode_changed", event.type)
        assertEquals("conversation", (event.data as JSONObject).optString("mode"))
    }

    @Test
    fun nullDataIsAccepted() {
        val event = parseWebSocketEvent("""{"protocol":1,"type":"connected","data":null}""")
        assertEquals("connected", event.type)
        assertNull(event.data)
    }

    @Test
    fun missingEventTypeIsProtocolError() {
        assertProtocolError { parseWebSocketEvent("""{"protocol":1,"data":{}}""") }
    }

    @Test
    fun unsupportedProtocolVersionIsProtocolError() {
        assertProtocolError { parseWebSocketEvent("""{"protocol":2,"type":"connected","data":{}}""") }
    }

    @Test
    fun arrayDataIsValidForCoreBroadcastEvents() {
        val event = parseWebSocketEvent("""{"protocol":1,"type":"agents_changed","data":[]}""")
        assertEquals("agents_changed", event.type)
        assertEquals("JSONArray", event.data?.javaClass?.simpleName)
    }
}
