package com.stripe.android

import androidx.annotation.VisibleForTesting
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import java.io.Closeable
import java.io.IOException
import java.net.HttpURLConnection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class StripeFireAndForgetRequestExecutor : FireAndForgetRequestExecutor {

    private val connectionFactory: ConnectionFactory = ConnectionFactory()

    /**
     * Make the request and ignore the response
     *
     * @return the response status code. Used for testing purposes.
     */
    @VisibleForTesting
    @Throws(APIConnectionException::class, InvalidRequestException::class)
    fun execute(request: StripeRequest): Int {
        // HttpURLConnection verifies SSL cert by default
        var conn: HttpURLConnection? = null
        try {
            conn = connectionFactory.create(request)

            // required to trigger the request
            val responseCode = conn.responseCode

            closeConnection(conn, responseCode)
            return responseCode
        } catch (e: IOException) {
            throw APIConnectionException.create(request.baseUrl, e)
        } finally {
            conn?.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun closeConnection(conn: HttpURLConnection, responseCode: Int) {
        if (responseCode in 200..299) {
            closeStream(conn.inputStream)
        } else {
            closeStream(conn.errorStream)
        }
    }

    @Throws(IOException::class)
    private fun closeStream(stream: Closeable?) {
        stream?.close()
    }

    override fun executeAsync(request: StripeRequest) {
        GlobalScope.launch {
            execute(request)
        }
    }
}
