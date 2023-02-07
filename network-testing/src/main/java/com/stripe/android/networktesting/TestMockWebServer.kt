package com.stripe.android.networktesting

import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

internal class TestMockWebServer {
    private val mockWebServer: MockWebServer = MockWebServer()
    private val localhostCertificate: HeldCertificate = localhostCertificate()

    val dispatcher: NetworkDispatcher = NetworkDispatcher()
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

    fun clientSocketFactory(): SSLSocketFactory {
        val handshakeCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(localhostCertificate.certificate)
            .build()

        val sslContext: SSLContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(handshakeCertificates.trustManager), SecureRandom())
        return sslContext.socketFactory
    }
}
