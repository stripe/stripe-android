package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

internal class ConnectionFactory {

    @Throws(IOException::class, InvalidRequestException::class)
    fun create(request: StripeRequest): HttpURLConnection {
        val stripeURL = URL(request.url)
        val conn = stripeURL.openConnection() as HttpURLConnection
        conn.connectTimeout = 30 * 1000
        conn.readTimeout = 80 * 1000
        conn.useCaches = false

        for ((key, value) in request.headers) {
            conn.setRequestProperty(key, value)
        }

        if (conn is HttpsURLConnection) {
            conn.sslSocketFactory = SSL_SOCKET_FACTORY
        }

        conn.requestMethod = request.method.code

        if (StripeRequest.Method.POST == request.method) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", request.contentType)
            conn.outputStream.use { output -> output.write(getRequestOutputBytes(request)) }
        }

        return conn
    }

    @Throws(InvalidRequestException::class)
    fun getRequestOutputBytes(request: StripeRequest): ByteArray {
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
    }
}
