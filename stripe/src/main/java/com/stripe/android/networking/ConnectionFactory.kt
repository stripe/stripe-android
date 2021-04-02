package com.stripe.android.networking

import com.stripe.android.exception.InvalidRequestException
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal interface ConnectionFactory {
    @Throws(IOException::class, InvalidRequestException::class)
    fun create(request: StripeRequest): StripeConnection

    class Default : ConnectionFactory {
        @Throws(IOException::class, InvalidRequestException::class)
        @JvmSynthetic
        override fun create(request: StripeRequest): StripeConnection {
            // HttpURLConnection verifies SSL cert by default
            val conn = openConnection(request.url).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                useCaches = false
                requestMethod = request.method.code

                request.headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }

                if (StripeRequest.Method.POST == request.method) {
                    doOutput = true
                    setRequestProperty(HEADER_CONTENT_TYPE, request.contentType)
                    outputStream.use { output -> request.writeBody(output) }
                }
            }

            return StripeConnection.Default(conn)
        }

        private fun openConnection(requestUrl: String): HttpsURLConnection {
            val httpsUrlConnection = URL(requestUrl).openConnection() as HttpsURLConnection
            val sslContext: SSLContext = SSLContext.getInstance("TLSv1.2")

            sslContext.init(null, null, null)
            httpsUrlConnection.sslSocketFactory = MySSLSocketFactory(sslContext.socketFactory)

            return httpsUrlConnection
        }

        class MySSLSocketFactory(private val wrappedSSLSocketFactory: SSLSocketFactory) :
            SSLSocketFactory() {

            override fun getDefaultCipherSuites(): Array<String> {
                return getMyCipherSuites()
            }

            override fun getSupportedCipherSuites(): Array<String> {
                return getMyCipherSuites()
            }

            override fun createSocket(
                socket: Socket,
                host: String,
                port: Int,
                autoClose: Boolean
            ): SSLSocket {
                val sslSocket =
                    wrappedSSLSocketFactory.createSocket(socket, host, port, autoClose) as SSLSocket

                // change the supported cipher suites on the socket, *before* it's returned to the client
                sslSocket.enabledCipherSuites = getMyCipherSuites()

                return sslSocket
            }

            override fun createSocket(host: String?, port: Int): Socket {
                val sslSocket = wrappedSSLSocketFactory.createSocket(host, port) as SSLSocket

                // change the supported cipher suites on the socket, *before* it's returned to the client
                sslSocket.enabledCipherSuites = getMyCipherSuites()

                return sslSocket
            }

            override fun createSocket(
                host: String?,
                port: Int,
                localHost: InetAddress?,
                localPort: Int
            ): Socket {
                val sslSocket =
                    wrappedSSLSocketFactory.createSocket(
                        host,
                        port,
                        localHost,
                        localPort
                    ) as SSLSocket

                // change the supported cipher suites on the socket, *before* it's returned to the client
                sslSocket.enabledCipherSuites = getMyCipherSuites()

                return sslSocket
            }

            override fun createSocket(host: InetAddress?, port: Int): Socket {
                val sslSocket =
                    wrappedSSLSocketFactory.createSocket(host, port) as SSLSocket

                // change the supported cipher suites on the socket, *before* it's returned to the client
                sslSocket.enabledCipherSuites = getMyCipherSuites()

                return sslSocket
            }

            override fun createSocket(
                address: InetAddress?,
                port: Int,
                localAddress: InetAddress?,
                localPort: Int
            ): Socket {
                val sslSocket =
                    wrappedSSLSocketFactory.createSocket(
                        address,
                        port,
                        localAddress,
                        localPort
                    ) as SSLSocket

                // change the supported cipher suites on the socket, *before* it's returned to the client
                sslSocket.enabledCipherSuites = getMyCipherSuites()

                return sslSocket
            }

            // Started with TLSv1.2 default and made to take the advisement of these sources:
            //  - https://tools.ietf.org/html/rfc7540#page-68
            //  - https://github.com/grpc/grpc/blob/6c56c5c00ba842459d9563075ad9fc6ce9b58b4a/src/core/lib/security/security_connector/ssl_utils.cc#L73
            private fun getMyCipherSuites(): Array<String> {
                return arrayOf(
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                )
            }
        }

        private companion object {
            private val CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(30).toInt()
            private val READ_TIMEOUT = TimeUnit.SECONDS.toMillis(80).toInt()

            private const val HEADER_CONTENT_TYPE = "Content-Type"
        }
    }
}
