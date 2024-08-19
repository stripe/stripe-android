package com.stripe.android.networktesting

import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration

internal class TestMockWebServer(validationTimeout: Duration?) {
    private val mockWebServer: MockWebServer = MockWebServer()
    private val localhostCertificate: HeldCertificate = localhostCertificate()

    val dispatcher: NetworkDispatcher = NetworkDispatcher(validationTimeout)
    val baseUrl: HttpUrl

    init {
        mockWebServer.dispatcher = dispatcher
        mockWebServer.useHttps(serverSocketFactory(), false)
        mockWebServer.start()
        baseUrl = mockWebServer.url("")
    }

    private fun localhostCertificate(): HeldCertificate {
        return HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .commonName("Test Fixture")
            .build()
    }

    private fun serverSocketFactory(): SSLSocketFactory {
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(localhostCertificate)
            .build()
        return serverCertificates.sslSocketFactory()
    }

    @Suppress("CustomX509TrustManager", "TrustAllX509TrustManager")
    fun clientSocketFactory(
        trustAll: Boolean = false,
    ): SSLSocketFactory {
        val trustManager = if (trustAll) {
            object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) = Unit

                override fun checkServerTrusted(
                    chain: Array<out X509Certificate>?,
                    authType: String?
                ) = Unit

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return emptyArray()
                }
            }
        } else {
            HandshakeCertificates.Builder()
                .addTrustedCertificate(localhostCertificate.certificate)
                .build()
                .trustManager
        }

        val sslContext: SSLContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())
        return sslContext.socketFactory
    }
}
