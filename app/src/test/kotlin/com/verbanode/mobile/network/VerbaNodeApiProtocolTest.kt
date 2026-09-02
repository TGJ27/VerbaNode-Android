package com.verbanode.mobile.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerbaNodeApiProtocolTest {
    private fun apiReturning(body: String, status: Int = 200): VerbaNodeApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message(if (status in 200..299) "OK" else "ERROR")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return VerbaNodeApi("https://verbanode.test", "unused", client)
    }

    private fun assertProtocolError(block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertNotNull("Expected protocol failure", error)
        assertEquals("ApiProtocolException", error!!.javaClass.simpleName)
    }

    @Test
    fun malformedObjectResponseIsProtocolError() {
        assertProtocolError { apiReturning("not-json").clientInfo() }
    }

    @Test
    fun objectReturnedWhereArrayExpectedIsProtocolError() {
        assertProtocolError { apiReturning("{\"items\":[]}").agentsRaw("session") }
    }

    @Test
    fun blankJsonResponseIsProtocolError() {
        assertProtocolError { apiReturning("").clientInfo() }
    }

    @Test
    fun malformedWebSocketTicketIsProtocolError() {
        assertProtocolError { apiReturning("{\"unexpected\":true}").wsTicket("session") }
    }

    @Test
    fun malformedPairingStartIsProtocolError() {
        assertProtocolError { apiReturning("{\"pairing_id\":\"pair-1\"}").startPairing("session") }
    }

    @Test
    fun structuredApiErrorKeepsStatusCodeAndMessage() {
        val api = apiReturning(
            """{"detail":"Incorrect PIN","error":{"code":"invalid_pin","message":"Incorrect PIN","request_id":"abc"}}""",
            status = 401,
        )
        val error = runCatching { api.pinLogin("0000", "Android") }.exceptionOrNull()
        assertTrue(error is ApiException)
        error as ApiException
        assertEquals(401, error.status)
        assertEquals("invalid_pin", error.code)
        assertEquals("Incorrect PIN", error.message)
    }
}
