package com.stripe.android

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Wraps a SSLSocketFactory and enables more TLS versions on older versions of Android.
 * Most of the code is taken from stripe-java.
 */
internal class StripeSSLSocketFactory constructor(
    private val tlsv11Supported: Boolean,
    private val tlsv12Supported: Boolean
) : SSLSocketFactory() {
    private val internalFactory: SSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory()

    internal constructor() : this(supportedProtocols = SUPPORTED_PROTOCOLS)

    private constructor(supportedProtocols: Array<String>) : this(
        tlsv11Supported = supportedProtocols.any { it == TLS_V11_PROTO },
        tlsv12Supported = supportedProtocols.any { it == TLS_V12_PROTO }
    )

    override fun getDefaultCipherSuites(): Array<String> {
        return internalFactory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return internalFactory.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(
        s: Socket,
        host: String,
        port: Int,
        autoClose: Boolean
    ): Socket? {
        return fixupSocket(internalFactory.createSocket(s, host, port, autoClose))
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket? {
        return fixupSocket(internalFactory.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket? {
        return fixupSocket(internalFactory.createSocket(host, port, localHost, localPort))
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return fixupSocket(internalFactory.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket? {
        return fixupSocket(
            internalFactory.createSocket(address, port, localAddress, localPort)
        )
    }

    private fun fixupSocket(sock: Socket): Socket {
        if (sock is SSLSocket) {
            sock.enabledProtocols = getEnabledProtocols(sock.enabledProtocols)
        }
        return sock
    }

    fun getEnabledProtocols(enabledProtocols: Array<String>) =
        listOfNotNull(
            *enabledProtocols,
            TLS_V11_PROTO.takeIf { tlsv11Supported },
            TLS_V12_PROTO.takeIf { tlsv12Supported }
        ).toSet().toTypedArray()

    private companion object {
        private const val TLS_V11_PROTO = "TLSv1.1"
        private const val TLS_V12_PROTO = "TLSv1.2"

        // For Android prior to 4.1, TLSv1.1 and TLSv1.2 might not be supported
        private val SUPPORTED_PROTOCOLS: Array<String>
            get() {
                return runCatching {
                    SSLContext.getDefault().supportedSSLParameters.protocols
                }.getOrDefault(emptyArray())
            }
    }
}
