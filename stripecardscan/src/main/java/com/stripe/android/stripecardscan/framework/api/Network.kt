package com.stripe.android.stripecardscan.framework.api

import android.util.Log
import com.stripe.android.camera.framework.time.Duration
import com.stripe.android.camera.framework.time.seconds
import com.stripe.android.core.utils.decodeFromJson
import com.stripe.android.core.utils.encodeToXWWWFormUrl
import com.stripe.android.stripecardscan.framework.api.StripeNetwork.Companion.RESPONSE_CODE_UNSET
import com.stripe.android.stripecardscan.framework.util.retry
import kotlinx.serialization.KSerializer
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream

private const val LOG_TAG = "Network"

private const val REQUEST_METHOD_GET = "GET"
private const val REQUEST_METHOD_POST = "POST"

private const val REQUEST_PROPERTY_AUTHORIZATION = "Authorization"
private const val REQUEST_PROPERTY_CONTENT_TYPE = "Content-Type"
private const val REQUEST_PROPERTY_CONTENT_ENCODING = "Content-Encoding"

private const val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded"
private const val CONTENT_ENCODING_GZIP = "gzip"

/**
 * The size of a TCP network packet. If smaller than this, there is no benefit to GZIP.
 */
private const val GZIP_MIN_SIZE_BYTES = 1500

internal interface Network {

    /**
     * Send a post request to a Stripe endpoint.
     */
    suspend fun <Request, Response, Error> postForResult(
        stripePublishableKey: String,
        path: String,
        data: Request,
        requestSerializer: KSerializer<Request>,
        responseSerializer: KSerializer<Response>,
        errorSerializer: KSerializer<Error>
    ): NetworkResult<out Response, out Error>

    /**
     * Send a post request to a Stripe endpoint and ignore the response.
     */
    suspend fun <Request> postData(
        stripePublishableKey: String,
        path: String,
        data: Request,
        requestSerializer: KSerializer<Request>
    )

    /**
     * Send a get request to a Stripe endpoint and parse the response.
     */
    suspend fun <Response, Error> getForResult(
        stripePublishableKey: String,
        path: String,
        responseSerializer: KSerializer<Response>,
        errorSerializer: KSerializer<Error>
    ): NetworkResult<out Response, out Error>

    /**
     * Download a file from Stripe endpoint
     */
    @Throws(IOException::class)
    suspend fun downloadFileWithRetries(
        url: URL,
        outputFile: File
    ): Int
}

/**
 * Legacy implementation of [Network], has support to [NetworkConfig.useCompression], kept for now
 * until [NetworkConfig.useCompression] is added to [StripeNetwork].
 *
 * TODO(ccen): Delete this class once [NetworkConfig.useCompression] is supported in [StripeNetwork].
 */
internal class LegacyStripeNetwork(
    baseUrl: String,
    private val retryDelayFunction: (attempt: Int, totalAttempts: Int) -> Duration,
    private val retryTotalAttempts: Int,
    private val retryStatusCodes: Iterable<Int>
) : Network {

    companion object {
        const val useCompression: Boolean = false
    }

    /**
     * Get the [baseUrl] with no trailing slashes.
     */
    private val baseUrl by lazy {
        if (baseUrl.endsWith("/")) {
            baseUrl.substring(0, baseUrl.length - 1)
        } else {
            baseUrl
        }
    }

    override suspend fun <Request, Response, Error> postForResult(
        stripePublishableKey: String,
        path: String,
        data: Request,
        requestSerializer: KSerializer<Request>,
        responseSerializer: KSerializer<Response>,
        errorSerializer: KSerializer<Error>
    ): NetworkResult<out Response, out Error> =
        translateNetworkResult(
            networkResult = postDataWithRetries(
                stripePublishableKey = stripePublishableKey,
                path = path,
                encodedData = encodeToXWWWFormUrl(requestSerializer, data)
            ),
            responseSerializer = responseSerializer,
            errorSerializer = errorSerializer
        )

    /**
     * Send a post request to a Stripe endpoint and ignore the response.
     */
    override suspend fun <Request> postData(
        stripePublishableKey: String,
        path: String,
        data: Request,
        requestSerializer: KSerializer<Request>
    ) {
        postDataWithRetries(
            stripePublishableKey = stripePublishableKey,
            path = path,
            encodedData = encodeToXWWWFormUrl(requestSerializer, data)
        )
    }

    /**
     * Send a get request to a Stripe endpoint and parse the response.
     */
    override suspend fun <Response, Error> getForResult(
        stripePublishableKey: String,
        path: String,
        responseSerializer: KSerializer<Response>,
        errorSerializer: KSerializer<Error>
    ): NetworkResult<out Response, out Error> =
        translateNetworkResult(
            getWithRetries(stripePublishableKey, path),
            responseSerializer,
            errorSerializer
        )

    override suspend fun downloadFileWithRetries(url: URL, outputFile: File): Int {
        // No-op, see the implementation in [StripeNetwork#downloadFileWithRetries]
        return RESPONSE_CODE_UNSET
    }

    /**
     * Send a post request to a Stripe endpoint with retries.
     */
    private suspend fun postDataWithRetries(
        stripePublishableKey: String,
        path: String,
        encodedData: String
    ): NetworkResult<out String, out String> =
        try {
            retry(
                retryDelayFunction = retryDelayFunction,
                times = retryTotalAttempts
            ) {
                val result = postData(stripePublishableKey, path, encodedData)
                if (result.responseCode in retryStatusCodes) {
                    throw RetryNetworkRequestException(result)
                } else {
                    result
                }
            }
        } catch (e: RetryNetworkRequestException) {
            e.result
        }

    /**
     * Send a get request to a Stripe endpoint with retries.
     */
    private suspend fun getWithRetries(
        stripePublishableKey: String,
        path: String
    ): NetworkResult<out String, out String> =
        try {
            retry(
                retryDelayFunction = retryDelayFunction,
                times = retryTotalAttempts
            ) {
                val result = get(stripePublishableKey, path)
                if (result.responseCode in retryStatusCodes) {
                    throw RetryNetworkRequestException(result)
                } else {
                    result
                }
            }
        } catch (e: RetryNetworkRequestException) {
            e.result
        }

    /**
     * Send a post request to a Stripe endpoint.
     */
    private fun postData(
        stripePublishableKey: String,
        path: String,
        encodedData: String
    ): NetworkResult<out String, out String> {
        val fullPath = if (path.startsWith("/")) path else "/$path"
        val url = URL("$baseUrl$fullPath")
        var responseCode = -1

        return try {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = REQUEST_METHOD_POST

                // Set the connection to both send and receive data
                doOutput = true
                doInput = true

                // Set headers
                setRequestHeaders(stripePublishableKey)
                setRequestProperty(REQUEST_PROPERTY_CONTENT_TYPE, CONTENT_TYPE_FORM)

                // Write the data
                if (useCompression && encodedData.toByteArray().size >= GZIP_MIN_SIZE_BYTES) {
                    setRequestProperty(REQUEST_PROPERTY_CONTENT_ENCODING, CONTENT_ENCODING_GZIP)
                    writeGzipData(
                        outputStream,
                        encodedData
                    )
                } else {
                    writeData(
                        outputStream,
                        encodedData
                    )
                }

                // Read the response code. This will block until the response has been received.
                responseCode = this.responseCode

                // Read the response
                when (responseCode) {
                    in 200 until 300 -> NetworkResult.Success(
                        responseCode,
                        readResponse(this)
                    )
                    else -> NetworkResult.Error(
                        responseCode,
                        readResponse(this)
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Failed network request to endpoint $url", t)
            NetworkResult.Exception(responseCode, t)
        }
    }

    /**
     * Send a get request to a Stripe endpoint.
     */
    private fun get(
        stripePublishableKey: String,
        path: String
    ): NetworkResult<out String, out String> {
        val fullPath = if (path.startsWith("/")) path else "/$path"
        val url = URL("$baseUrl$fullPath")
        var responseCode = -1

        return try {
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = REQUEST_METHOD_GET

                // Set the connection to only receive data
                doOutput = false
                doInput = true

                // Set headers
                setRequestHeaders(stripePublishableKey)

                // Read the response code. This will block until the response has been received.
                responseCode = this.responseCode

                // Read the response
                when (responseCode) {
                    in 200 until 300 -> NetworkResult.Success(
                        responseCode,
                        readResponse(this)
                    )
                    else -> NetworkResult.Error(
                        responseCode,
                        readResponse(this)
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Failed network request to endpoint $url", t)
            NetworkResult.Exception(responseCode, t)
        }
    }
}

/**
 * Translate a string network result to a response or error.
 */
private fun <Response, Error> translateNetworkResult(
    networkResult: NetworkResult<out String, out String>,
    responseSerializer: KSerializer<Response>,
    errorSerializer: KSerializer<Error>
): NetworkResult<out Response, out Error> = when (networkResult) {
    is NetworkResult.Success ->
        try {
            NetworkResult.Success(
                responseCode = networkResult.responseCode,
                body = decodeFromJson(responseSerializer, networkResult.body)
            )
        } catch (t: Throwable) {
            try {
                NetworkResult.Error(
                    responseCode = networkResult.responseCode,
                    error = decodeFromJson(errorSerializer, networkResult.body)
                )
            } catch (et: Throwable) {
                NetworkResult.Exception(networkResult.responseCode, t)
            }
        }
    is NetworkResult.Error ->
        try {
            NetworkResult.Error(
                responseCode = networkResult.responseCode,
                error = decodeFromJson(errorSerializer, networkResult.error)
            )
        } catch (t: Throwable) {
            NetworkResult.Exception(networkResult.responseCode, t)
        }
    is NetworkResult.Exception ->
        NetworkResult.Exception(
            responseCode = networkResult.responseCode,
            exception = networkResult.exception
        )
}

// TODO(ccen): Replace its caller with [StripeNetwork#downloadFileWithRetries]
@Throws(IOException::class)
internal suspend fun downloadFileWithRetries(
    url: URL,
    outputFile: File,
    retryDelayFunction: (attempt: Int, totalAttempts: Int) -> Duration = { _, _ -> 3.seconds }
) = retry(
    retryDelayFunction,
    excluding = listOf(FileNotFoundException::class.java)
) {
    downloadFile(url, outputFile)
}

/**
 * Download a file.
 */
@Throws(IOException::class)
private fun downloadFile(
    url: URL,
    outputFile: File
) = try {
    with(url.openConnection() as HttpURLConnection) {
        requestMethod = REQUEST_METHOD_GET

        // Set the connection to only receive data
        doOutput = false
        doInput = true

        // Read the response code. This will block until the response has been received.
        val responseCode = this.responseCode

        inputStream.use { stream ->
            FileOutputStream(outputFile).use { stream.copyTo(it) }
        }

        responseCode
    }
} catch (t: Throwable) {
    Log.w(LOG_TAG, "Failed network request to endpoint $url", t)
    throw t
}

/**
 * Set the required request headers on an HttpURLConnection
 */
private fun HttpURLConnection.setRequestHeaders(stripePublishableKey: String) {
    setRequestProperty(REQUEST_PROPERTY_AUTHORIZATION, "Bearer $stripePublishableKey")
}

private fun writeGzipData(outputStream: OutputStream, data: String) {
    OutputStreamWriter(
        GZIPOutputStream(
            outputStream
        )
    ).use {
        it.write(data)
        it.flush()
    }
}

private fun writeData(outputStream: OutputStream, data: String) {
    OutputStreamWriter(outputStream).use {
        it.write(data)
        it.flush()
    }
}

private fun readResponse(connection: HttpURLConnection): String =
    InputStreamReader(connection.inputStream).use {
        it.readLines().joinToString(separator = "\n")
    }

/**
 * An exception that should never be thrown, but is required for typing.
 */
private class RetryNetworkRequestException(val result: NetworkResult<out String, out String>) :
    Exception()
