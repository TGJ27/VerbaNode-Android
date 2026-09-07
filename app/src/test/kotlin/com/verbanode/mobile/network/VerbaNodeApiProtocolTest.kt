package com.verbanode.mobile.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class VerbaNodeApiProtocolTest {

    private fun authGrant(token: String = "session-token"): JSONObject = JSONObject()
        .put("token", token)
        .put("server_version", "0.12.2")
        .put("api_version", 1)
        .put("websocket_protocol_version", 1)
        .put("heartbeat_interval_seconds", 15.0)
        .put("heartbeat_timeout_seconds", 45.0)
        .put("session", JSONObject().put("session_id", "session-1").put("client_name", "Pixel"))

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
    @Test
    fun authenticatedRequestSendsSessionTokenHeader() {
        val seen = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                seen.set(chain.request())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        JSONObject()
                            .put("agents", JSONArray())
                            .put("messages", JSONArray())
                            .put("mode", "idle")
                            .toString()
                            .toResponseBody("application/json".toMediaType()),
                    )
                    .build()
            }
            .build()
        val api = VerbaNodeApi("https://verbanode.test", "unused", client)

        api.bootstrap("session-123")

        assertEquals("session-123", seen.get().header("X-Session-Token"))
        assertEquals("/api/bootstrap", seen.get().url.encodedPath)
    }

    @Test
    fun pinLoginSendsMobileProtocolContract() {
        val seen = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                seen.set(chain.request())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        authGrant().toString()
                            .toResponseBody("application/json".toMediaType()),
                    )
                    .build()
            }
            .build()
        val api = VerbaNodeApi("https://verbanode.test", "unused", client)

        val session = api.pinLogin("123456", "Pixel")

        val request = seen.get()
        val payload = JSONObject(request.body!!.let { body ->
            val buffer = okio.Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        })
        assertEquals("POST", request.method)
        assertEquals("/api/auth/login", request.url.encodedPath)
        assertEquals("123456", payload.getString("pin"))
        assertEquals("Pixel", payload.getString("client_name"))
        assertEquals("mobile", payload.getString("client_type"))
        assertEquals(1, payload.getInt("api_version"))
        assertEquals("session-token", session.token)
    }


    @Test
    fun authGrantRequiresProtocolAndHeartbeatMetadata() {
        assertProtocolError { apiReturning("{\"token\":\"session-token\"}").pinLogin("123456", "Pixel") }
    }

    @Test
    fun conversationRequiresMessagesArray() {
        assertProtocolError {
            apiReturning("{\"id\":1}").conversation("session", 1)
        }
    }

    @Test
    fun audioUploadStreamsSelectedFileInsteadOfPrebufferingIt() {
        val opens = AtomicInteger(0)
        val captured = AtomicReference<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                assertEquals(0, opens.get())
                assertTrue(request.body!!.contentLength() > 5L)
                assertEquals(0, opens.get())
                val buffer = okio.Buffer()
                request.body!!.writeTo(buffer)
                captured.set(buffer.readUtf8())
                assertEquals(1, opens.get())
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val api = VerbaNodeApi("https://verbanode.test", "unused", client)

        api.uploadAudio("session", "clip.mp3", "audio/mpeg", 5L) {
            opens.incrementAndGet()
            ByteArrayInputStream("hello".toByteArray())
        }

        assertTrue(captured.get().contains("clip.mp3"))
        assertTrue(captured.get().contains("hello"))
    }

    @Test
    fun backupRestoreStreamsSelectedArchiveInsteadOfPrebufferingIt() {
        val opens = AtomicInteger(0)
        val seen = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                seen.set(request)
                assertEquals(0, opens.get())
                val buffer = okio.Buffer()
                request.body!!.writeTo(buffer)
                assertEquals(1, opens.get())
                assertTrue(buffer.readUtf8().contains("backup-bytes"))
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val api = VerbaNodeApi("https://verbanode.test", "unused", client)

        api.restoreBackup("session", "backup.zip", 12L) {
            opens.incrementAndGet()
            ByteArrayInputStream("backup-bytes".toByteArray())
        }

        assertEquals("POST", seen.get().method)
        assertEquals("/api/restore", seen.get().url.encodedPath)
    }

    @Test
    fun knowledgeCatalogCanRequestAllDocumentsWithoutLibraryFilter() {
        val seen = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                seen.set(chain.request())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("[]".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val api = VerbaNodeApi("https://verbanode.test", "unused", client)

        api.knowledgeDocuments("session")

        assertEquals("/api/knowledge/documents", seen.get().url.encodedPath)
        assertEquals(null, seen.get().url.query)
    }

    @Test
    fun selectedKnowledgeLibraryRequestKeepsLibraryFilter() {
        val seen = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                seen.set(chain.request())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("[]".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val api = VerbaNodeApi("https://verbanode.test", "unused", client)

        api.knowledgeDocuments("session", 7)

        assertEquals("/api/knowledge/documents", seen.get().url.encodedPath)
        assertEquals("library_id=7", seen.get().url.query)
    }

}
