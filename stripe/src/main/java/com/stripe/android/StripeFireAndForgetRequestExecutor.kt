package com.stripe.android

import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.VisibleForTesting
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import java.io.Closeable
import java.io.IOException
import java.net.HttpURLConnection

internal class StripeFireAndForgetRequestExecutor : FireAndForgetRequestExecutor {

    private val connectionFactory: ConnectionFactory = ConnectionFactory()
    private val handler: Handler = createHandler()

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
        handler.post {
            try {
                execute(request)
            } catch (ignore: Exception) {
            }
        }
    }

    companion object {
        private fun createHandler(): Handler {
            val handlerThread = HandlerThread(
                StripeFireAndForgetRequestExecutor::class.java.simpleName
            )
            handlerThread.start()
            return Handler(handlerThread.looper)
        }
    }
}
