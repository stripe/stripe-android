package com.stripe.android

import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Scanner

/**
 * Used by [StripeApiRepository] to make HTTP requests
 */
internal class StripeApiRequestExecutor : ApiRequestExecutor {
    private val connectionFactory: ConnectionFactory = ConnectionFactory()

    /**
     * Make the request and return the response as a [StripeResponse]
     */
    @Throws(APIConnectionException::class, InvalidRequestException::class)
    override fun execute(request: ApiRequest): StripeResponse {
        // HttpURLConnection verifies SSL cert by default
        var conn: HttpURLConnection? = null
        try {
            conn = connectionFactory.create(request)
            // trigger the request
            val responseCode = conn.responseCode
            val responseBody = if (responseCode in 200..299) {
                getResponseBody(conn.inputStream)
            } else {
                getResponseBody(conn.errorStream)
            }
            return StripeResponse(responseCode, responseBody, conn.headerFields)
        } catch (e: IOException) {
            throw APIConnectionException.create(request.baseUrl, e)
        } finally {
            conn?.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun getResponseBody(responseStream: InputStream?): String? {
        if (responseStream == null) {
            return null
        }

        // \A is the beginning of the stream boundary
        val scanner = Scanner(responseStream, CHARSET).useDelimiter("\\A")
        val responseBody = if (scanner.hasNext()) scanner.next() else null
        responseStream.close()
        return responseBody
    }

    companion object {
        private val CHARSET = StandardCharsets.UTF_8.name()
    }
}
