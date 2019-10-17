package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URL
import java.nio.charset.StandardCharsets
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

            for ((key, value) in request.headers) {
                setRequestProperty(key, value)
            }

            if (StripeRequest.Method.POST == request.method) {
                doOutput = true
                setRequestProperty("Content-Type", request.contentType)
                outputStream.use { output -> output.write(getRequestOutputBytes(request)) }
            }
        }

        return StripeConnection(conn)
    }

    private fun openConnection(requestUrl: String): HttpsURLConnection {
        return URL(requestUrl).openConnection() as HttpsURLConnection
    }

    @Throws(InvalidRequestException::class)
    @JvmSynthetic
    internal fun getRequestOutputBytes(request: StripeRequest): ByteArray {
        try {
            return request.getOutputBytes()
        } catch (e: UnsupportedEncodingException) {
            throw InvalidRequestException(
                "Unable to encode parameters to ${StandardCharsets.UTF_8.name()}. " +
                    "Please contact support@stripe.com for assistance.",
                null, null, 0, null, null, null, e
            )
        }
    }

    companion object {
        private val SSL_SOCKET_FACTORY = StripeSSLSocketFactory()
        private val CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(30).toInt()
        private val READ_TIMEOUT = TimeUnit.SECONDS.toMillis(80).toInt()
    }
}
