package com.stripe.android.cardverificationsheet.framework.api

import android.util.Log
import com.stripe.android.cardverificationsheet.framework.Config
import com.stripe.android.cardverificationsheet.framework.NetworkConfig
import com.stripe.android.cardverificationsheet.framework.time.Timer
import com.stripe.android.cardverificationsheet.framework.util.retry
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

private const val REQUEST_METHOD_GET = "GET"
private const val REQUEST_METHOD_POST = "POST"

private const val REQUEST_PROPERTY_AUTHENTICATION = "x-stripe-auth"
private const val REQUEST_PROPERTY_CONTENT_TYPE = "Content-Type"
private const val REQUEST_PROPERTY_CONTENT_ENCODING = "Content-Encoding"

private const val CONTENT_TYPE_JSON = "application/json; utf-8"
private const val CONTENT_ENCODING_GZIP = "gzip"

/**
 * The size of a TCP network packet. If smaller than this, there is no benefit to GZIP.
 */
private const val GZIP_MIN_SIZE_BYTES = 1500

private val networkTimer by lazy { Timer.newInstance(Config.logTag, "network") }

/**
 * Send a post request to a Stripe endpoint.
 */
internal suspend fun <Request, Response, Error> postForResult(
    stripePublishableKey: String,
    path: String,
    data: Request,
    requestSerializer: KSerializer<Request>,
    responseSerializer: KSerializer<Response>,
    errorSerializer: KSerializer<Error>
): NetworkResult<out Response, out Error> =
    translateNetworkResult(
        networkResult = postJsonWithRetries(
            stripePublishableKey = stripePublishableKey,
            path = path,
            jsonData = NetworkConfig.json.encodeToString(requestSerializer, data)
        ),
        responseSerializer = responseSerializer,
        errorSerializer = errorSerializer
    )

/**
 * Send a post request to a Stripe endpoint and ignore the response.
 */
internal suspend fun <Request> postData(
    stripePublishableKey: String,
    path: String,
    data: Request,
    requestSerializer: KSerializer<Request>
) {
    postJsonWithRetries(
        stripePublishableKey = stripePublishableKey,
        path = path,
        jsonData = NetworkConfig.json.encodeToString(requestSerializer, data)
    )
}

/**
 * Send a get request to a Stripe endpoint and parse the response.
 */
internal suspend fun <Response, Error> getForResult(
    stripePublishableKey: String,
    path: String,
    responseSerializer: KSerializer<Response>,
    errorSerializer: KSerializer<Error>
): NetworkResult<out Response, out Error> =
    translateNetworkResult(
        getWithRetries(stripePublishableKey, path),
        responseSerializer,
        errorSerializer,
    )

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
                body = NetworkConfig.json.decodeFromString(responseSerializer, networkResult.body)
            )
        } catch (t: Throwable) {
            try {
                NetworkResult.Error(
                    responseCode = networkResult.responseCode,
                    error = NetworkConfig.json.decodeFromString(errorSerializer, networkResult.body)
                )
            } catch (et: Throwable) {
                NetworkResult.Exception(networkResult.responseCode, t)
            }
        }
    is NetworkResult.Error ->
        try {
            NetworkResult.Error(
                responseCode = networkResult.responseCode,
                error = NetworkConfig.json.decodeFromString(errorSerializer, networkResult.error)
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

/**
 * Send a post request to a Stripe endpoint with retries.
 */
private suspend fun postJsonWithRetries(
    stripePublishableKey: String,
    path: String,
    jsonData: String
): NetworkResult<out String, out String> =
    try {
        retry(
            retryDelay = NetworkConfig.retryDelay,
            times = NetworkConfig.retryTotalAttempts
        ) {
            val result = postJson(stripePublishableKey, path, jsonData)
            if (result.responseCode in NetworkConfig.retryStatusCodes) {
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
    path: String,
): NetworkResult<out String, out String> =
    try {
        retry(
            retryDelay = NetworkConfig.retryDelay,
            times = NetworkConfig.retryTotalAttempts
        ) {
            val result = get(stripePublishableKey, path)
            if (result.responseCode in NetworkConfig.retryStatusCodes) {
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
private fun postJson(
    stripePublishableKey: String,
    path: String,
    jsonData: String
): NetworkResult<out String, out String> = networkTimer.measure(path) {
    val fullPath = if (path.startsWith("/")) path else "/$path"
    val url = URL("${getBaseUrl()}$fullPath")
    var responseCode = -1

    try {
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = REQUEST_METHOD_POST

            // Set the connection to both send and receive data
            doOutput = true
            doInput = true

            // Set headers
            setRequestHeaders(stripePublishableKey)
            setRequestProperty(REQUEST_PROPERTY_CONTENT_TYPE, CONTENT_TYPE_JSON)

            // Write the data
            if (NetworkConfig.useCompression &&
                jsonData.toByteArray().size >= GZIP_MIN_SIZE_BYTES
            ) {
                setRequestProperty(REQUEST_PROPERTY_CONTENT_ENCODING, CONTENT_ENCODING_GZIP)
                writeGzipData(
                    outputStream,
                    jsonData
                )
            } else {
                writeData(
                    outputStream,
                    jsonData
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
        Log.w(Config.logTag, "Failed network request to endpoint $url", t)
        NetworkResult.Exception(responseCode, t)
    }
}

/**
 * Send a get request to a Stripe endpoint.
 */
private fun get(
    stripePublishableKey: String,
    path: String,
): NetworkResult<out String, out String> = networkTimer.measure(path) {
    val fullPath = if (path.startsWith("/")) path else "/$path"
    val url = URL("${getBaseUrl()}$fullPath")
    var responseCode = -1

    try {
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
        Log.w(Config.logTag, "Failed network request to endpoint $url", t)
        NetworkResult.Exception(responseCode, t)
    }
}

@Throws(IOException::class)
internal suspend fun downloadFileWithRetries(url: URL, outputFile: File) = retry(
    NetworkConfig.retryDelay,
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
    outputFile: File,
) = networkTimer.measure(url.toString()) {
    try {
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
        Log.w(Config.logTag, "Failed network request to endpoint $url", t)
        throw t
    }
}

/**
 * Set the required request headers on an HttpURLConnection
 */
private fun HttpURLConnection.setRequestHeaders(stripePublishableKey: String) {
    setRequestProperty(REQUEST_PROPERTY_AUTHENTICATION, stripePublishableKey)
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
 * Get the [NetworkConfig.baseUrl] with no trailing slashes.
 */
private fun getBaseUrl() = if (NetworkConfig.baseUrl.endsWith("/")) {
    NetworkConfig.baseUrl.substring(0, NetworkConfig.baseUrl.length - 1)
} else {
    NetworkConfig.baseUrl
}

/**
 * An exception that should never be thrown, but is required for typing.
 */
private class RetryNetworkRequestException(val result: NetworkResult<out String, out String>) :
    Exception()
