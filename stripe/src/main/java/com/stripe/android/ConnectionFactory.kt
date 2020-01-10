package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

internal class ConnectionFactory {
    @Throws(IOException::class, InvalidRequestException::class)
    @JvmSynthetic
    internal fun create(request: StripeRequest): StripeConnection {
        // HttpURLConnection verifies SSL cert by default
        val conn = openConnection(request.url).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            useCaches = false
            sslSocketFactory = SSL_SOCKET_FACTORY
            requestMethod = request.method.code

            request.headers.forEach { (key, value) ->
                setRequestProperty(key, value)
            }

            if (StripeRequest.Method.POST == request.method) {
                doOutput = true
                setRequestProperty(HEADER_CONTENT_TYPE, request.contentType)
                outputStream.use { output -> output.write(request.bodyBytes) }
            }
        }

        return StripeConnection(conn)
    }

    private fun openConnection(requestUrl: String): HttpsURLConnection {
        return URL(requestUrl).openConnection() as HttpsURLConnection
    }

    private companion object {
        private val SSL_SOCKET_FACTORY = StripeSSLSocketFactory()
        private val CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(30).toInt()
        private val READ_TIMEOUT = TimeUnit.SECONDS.toMillis(80).toInt()

        private const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
