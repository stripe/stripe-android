package com.stripe.android

import androidx.annotation.VisibleForTesting
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import java.io.Closeable
import java.io.IOException
import java.net.HttpURLConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class StripeFireAndForgetRequestExecutor(
    private val logger: Logger = Logger.noop()
) : FireAndForgetRequestExecutor {

    private val connectionFactory: ConnectionFactory = ConnectionFactory()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    /**
     * Make the request and ignore the response
     *
     * @return the response status code. Used for testing purposes.
     */
    @VisibleForTesting
    @Throws(APIConnectionException::class, InvalidRequestException::class)
    internal fun execute(request: StripeRequest): Int {
        // HttpURLConnection verifies SSL cert by default
        var conn: HttpURLConnection? = null
        val responseCode: Int
        try {
            conn = connectionFactory.create(request)

            // required to trigger the request
            responseCode = conn.responseCode

            closeConnection(conn, responseCode)
        } catch (e: IOException) {
            throw APIConnectionException.create(request.baseUrl, e)
        } finally {
            conn?.disconnect()
        }

        return responseCode
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
        scope.launch {
            try {
                coroutineScope {
                    execute(request)
                }
            } catch (e: Exception) {
                logger.error("Exception while making fire-and-forget request", e)
            }
        }
    }
}
