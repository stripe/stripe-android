package com.stripe.android

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Scanner
import javax.net.ssl.HttpsURLConnection

/**
 * A wrapper for accessing a [HttpURLConnection]. Implements [Closeable] to simplify closing related
 * resources.
 */
internal class StripeConnection internal constructor(
    private val conn: HttpsURLConnection
) : Closeable {
    internal val responseCode: Int
        @JvmSynthetic
        get() {
            return conn.responseCode
        }

    internal val response: StripeResponse
        @Throws(IOException::class)
        @JvmSynthetic
        get() {
            // trigger the request
            val responseCode = this.responseCode
            return StripeResponse(responseCode, responseBody, conn.headerFields)
        }

    private val responseBody: String?
        @Throws(IOException::class)
        get() {
            return getResponseBody(responseStream)
        }

    private val responseStream: InputStream?
        @Throws(IOException::class)
        get() {
            return if (responseCode in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream
            }
        }

    @Throws(IOException::class)
    private fun getResponseBody(responseStream: InputStream?): String? {
        if (responseStream == null) {
            return null
        }

        // \A is the beginning of the stream boundary
        val scanner = Scanner(responseStream, CHARSET).useDelimiter("\\A")
        val responseBody = if (scanner.hasNext()) {
            scanner.next()
        } else {
            null
        }
        responseStream.close()
        return responseBody
    }

    override fun close() {
        responseStream?.close()
        conn.disconnect()
    }

    private companion object {
        private val CHARSET = StandardCharsets.UTF_8.name()
    }
}
