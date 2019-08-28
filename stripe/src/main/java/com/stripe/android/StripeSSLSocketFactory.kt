package com.stripe.android

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.NoSuchAlgorithmException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Wraps a SSLSocketFactory and enables more TLS versions on older versions of Android.
 * Most of the code is taken from stripe-java.
 */
internal class StripeSSLSocketFactory constructor(
    private val under: SSLSocketFactory,
    private val tlsv11Supported: Boolean,
    private val tlsv12Supported: Boolean
) : SSLSocketFactory() {
    constructor() : this(getSupportedProtocols())

    constructor(supportedProtocols: Array<String>) : this(
        tlsv11Supported = supportedProtocols.any { it == TLS_V11_PROTO },
        tlsv12Supported = supportedProtocols.any { it == TLS_V12_PROTO }
    )

    constructor(tlsv11Supported: Boolean, tlsv12Supported: Boolean) : this(
        under = HttpsURLConnection.getDefaultSSLSocketFactory(),
        tlsv11Supported = tlsv11Supported,
        tlsv12Supported = tlsv12Supported
    )

    override fun getDefaultCipherSuites(): Array<String> {
        return this.under.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return this.under.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(
        s: Socket,
        host: String,
        port: Int,
        autoClose: Boolean
    ): Socket? {
        return fixupSocket(this.under.createSocket(s, host, port, autoClose))
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket? {
        return fixupSocket(this.under.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket? {
        return fixupSocket(this.under.createSocket(host, port, localHost, localPort))
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return fixupSocket(this.under.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket? {
        return fixupSocket(
            this.under.createSocket(address, port, localAddress, localPort))
    }

    private fun fixupSocket(sock: Socket): Socket {
        if (sock is SSLSocket) {
            sock.enabledProtocols = getEnabledProtocols(sock.enabledProtocols)
        }
        return sock
    }

    fun getEnabledProtocols(
        enabledProtocols: Array<String>
    ): Array<String?> {
        return setOf(
            *enabledProtocols,
            if (tlsv11Supported) TLS_V11_PROTO else null,
            if (tlsv12Supported) TLS_V12_PROTO else null
        ).filterNotNull().toTypedArray()
    }

    companion object {
        private const val TLS_V11_PROTO = "TLSv1.1"
        private const val TLS_V12_PROTO = "TLSv1.2"

        // For Android prior to 4.1, TLSv1.1 and TLSv1.2 might not be supported
        fun getSupportedProtocols(): Array<String> {
            return try {
                SSLContext.getDefault().supportedSSLParameters.protocols
            } catch (e: NoSuchAlgorithmException) {
                emptyArray()
            }
        }
    }
}
