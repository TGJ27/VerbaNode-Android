package com.verbanode.mobile.network

import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


data class ProbeResult(
    val baseUrl: String,
    val clientInfo: ClientInfo,
    val certificateFingerprintSha256: String,
    val certificateSpkiSha256: String,
)

object TlsTrust {
    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun certificateFingerprint(certificate: X509Certificate): String =
        hex(MessageDigest.getInstance("SHA-256").digest(certificate.encoded))

    fun spkiFingerprint(certificate: X509Certificate): String =
        hex(MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded))

    private class CapturingTrustManager : X509TrustManager {
        val leaf = AtomicReference<X509Certificate?>(null)

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            val certificate = chain?.firstOrNull()
                ?: throw CertificateException("VerbaNode did not present an HTTPS certificate")
            certificate.checkValidity()
            leaf.set(certificate)
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /**
     * Performs an unauthenticated TOFU probe. The permissive trust manager is
     * intentionally used only for the public compatibility request and never
     * sends a PIN, session token, or trusted-device credential.
     *
     * Android/OkHttp does not guarantee that Response.handshake exposes the
     * peer chain for every custom TrustManager implementation. The trust manager
     * therefore captures the certificate directly during the TLS handshake.
     */
    fun probe(rawBaseUrl: String, expectedSpkiSha256: String? = null): ProbeResult {
        val baseUrl = normalizeTlsBaseUrl(rawBaseUrl)
        val trust = CapturingTrustManager()
        val ssl = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trust), SecureRandom())
        }
        val client = OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trust)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url("$baseUrl/api/client-info").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ApiException(response.code, null, "Compatibility check failed (${response.code})")
            }
            val certificate = trust.leaf.get()
                ?: (response.handshake?.peerCertificates?.firstOrNull() as? X509Certificate)
                ?: error("VerbaNode HTTPS identity could not be read")
            val body = response.body.string()
            val info = parseClientInfo(parseObjectResponse(body, "/api/client-info"))
            info.requireAndroidCompatibility()
            val spki = spkiFingerprint(certificate)
            val expected = expectedSpkiSha256?.takeIf { it.isNotBlank() }?.let(::normalizeSpkiSha256)
            if (expected != null && expected != spki) {
                error("The discovered VerbaNode identity changed before connection")
            }
            if (info.certificateSpkiSha256.isNotBlank() && info.certificateSpkiSha256 != spki) {
                error("VerbaNode reported a certificate identity that does not match its HTTPS certificate")
            }
            return ProbeResult(
                baseUrl = baseUrl,
                clientInfo = info,
                certificateFingerprintSha256 = certificateFingerprint(certificate),
                certificateSpkiSha256 = spki,
            )
        }
    }

    fun pinnedClient(expectedSpkiSha256: String): OkHttpClient {
        val expected = normalizeSpkiSha256(expectedSpkiSha256)
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val leaf = chain?.firstOrNull() ?: throw CertificateException("Missing server certificate")
                leaf.checkValidity()
                val actual = spkiFingerprint(leaf)
                if (!MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())) {
                    throw CertificateException("VerbaNode certificate identity changed")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val ssl = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trust), SecureRandom())
        }
        return OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trust)
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(130, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }
}
