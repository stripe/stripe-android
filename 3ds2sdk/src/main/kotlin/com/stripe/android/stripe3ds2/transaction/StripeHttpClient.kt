package com.stripe.android.stripe3ds2.transaction

import androidx.annotation.VisibleForTesting
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext

/**
 * Handles making GET and POST requests using [HttpURLConnection].
 */
internal class StripeHttpClient(
    private val url: String,
    private val connectionFactory: ConnectionFactory = DefaultConnectionFactory(),
    private val errorReporter: ErrorReporter,
    private val workContext: CoroutineContext
) : HttpClient {

    override suspend fun doGetRequest(): InputStream? = withContext(workContext) {
        runCatching {
            val connection = createGetConnection().also {
                it.connect()
            }
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream
                else -> null
            }
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrNull()
    }

    override suspend fun doPostRequest(
        requestBody: String,
        contentType: String
    ): HttpResponse = withContext(workContext) {
        runCatching {
            doPostRequestInternal(requestBody, contentType)
        }.onFailure {
            errorReporter.reportError(it)
        }.getOrElse {
            throw SDKRuntimeException(it)
        }
    }

    private fun doPostRequestInternal(
        requestBody: String,
        contentType: String
    ) = createPostConnection(requestBody, contentType).let { connection ->
        connection.outputStream.use { os ->
            os.writer(StandardCharsets.UTF_8).use { osw ->
                osw.write(requestBody)
                osw.flush()
            }
        }

        connection.connect()

        handlePostResponse(connection)
    }

    @VisibleForTesting
    internal fun handlePostResponse(conn: HttpURLConnection): HttpResponse {
        val responseCode = conn.responseCode
        val isSuccessfulResponse = isSuccessfulResponse(responseCode)
        if (!isSuccessfulResponse) {
            throw SDKRuntimeException("Unsuccessful response code from $url: $responseCode")
        }

        return HttpResponse(
            getResponseBody(conn.inputStream),
            conn.contentType
        )
    }

    private fun isSuccessfulResponse(responseCode: Int): Boolean {
        return responseCode in 200..299
    }

    private fun getResponseBody(
        inputStream: InputStream
    ) = runCatching {
        inputStream.bufferedReader().use(BufferedReader::readText)
    }.getOrNull().orEmpty()

    private fun createPostConnection(
        requestBody: String,
        contentType: String
    ) = createConnection().apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", contentType)
        setRequestProperty("Content-Length", requestBody.length.toString())
    }

    private fun createGetConnection() = createConnection().apply {
        doInput = true
    }

    private fun createConnection() = connectionFactory.create(url)

    interface ConnectionFactory {
        fun create(url: String): HttpURLConnection
    }

    private class DefaultConnectionFactory : ConnectionFactory {
        override fun create(url: String): HttpURLConnection {
            return URL(url).openConnection() as HttpURLConnection
        }
    }
}
