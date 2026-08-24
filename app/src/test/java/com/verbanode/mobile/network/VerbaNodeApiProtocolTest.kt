package com.verbanode.mobile.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertThrows
import org.junit.Test

class VerbaNodeApiProtocolTest {
    private fun apiReturning(body: String): VerbaNodeApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return VerbaNodeApi("https://verbanode.test", "unused", client)
    }

    @Test
    fun malformedObjectResponseIsProtocolError() {
        val api = apiReturning("not-json")
        assertThrows(ApiProtocolException::class.java) { api.clientInfo() }
    }

    @Test
    fun objectReturnedWhereArrayExpectedIsProtocolError() {
        val api = apiReturning("{\"items\":[]}")
        assertThrows(ApiProtocolException::class.java) { api.agentsRaw("session") }
    }

    @Test
    fun malformedWebSocketTicketIsProtocolError() {
        val api = apiReturning("{\"unexpected\":true}")
        assertThrows(ApiProtocolException::class.java) { api.wsTicket("session") }
    }
}
