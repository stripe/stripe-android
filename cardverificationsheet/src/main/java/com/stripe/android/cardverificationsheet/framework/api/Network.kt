package com.stripe.android.cardverificationsheet.framework.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.stripe.android.cardverificationsheet.framework.Config
import com.stripe.android.cardverificationsheet.framework.NetworkConfig
import com.stripe.android.cardverificationsheet.framework.time.Timer
import com.stripe.android.cardverificationsheet.framework.util.DeviceIds
import com.stripe.android.cardverificationsheet.framework.util.cacheFirstResult
import com.stripe.android.cardverificationsheet.framework.util.getAppPackageName
import com.stripe.android.cardverificationsheet.framework.util.getDeviceName
import com.stripe.android.cardverificationsheet.framework.util.getOsVersion
import com.stripe.android.cardverificationsheet.framework.util.getPlatform
import com.stripe.android.cardverificationsheet.framework.util.getSdkFlavor
import com.stripe.android.cardverificationsheet.framework.util.getSdkVersion
import com.stripe.android.cardverificationsheet.framework.util.retry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
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

private const val REQUEST_PROPERTY_AUTHENTICATION = "x-bouncer-auth"
private const val REQUEST_PROPERTY_DEVICE_ID = "x-bouncer-device-id"
private const val REQUEST_PROPERTY_USER_AGENT = "User-Agent"
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
 * Send a post request to a bouncer endpoint.
 */
internal suspend fun <Request, Response, Error> postForResult(
    context: Context,
    path: String,
    data: Request,
    requestSerializer: KSerializer<Request>,
    responseSerializer: KSerializer<Response>,
    errorSerializer: KSerializer<Error>
): NetworkResult<out Response, out Error> =
    translateNetworkResult(
        networkResult = postJsonWithRetries(
            context = context,
            path = path,
            jsonData = Config.json.encodeToString(requestSerializer, data)
        ),
        responseSerializer = responseSerializer,
        errorSerializer = errorSerializer
    )

/**
 * Send a post request to a bouncer endpoint and ignore the response.
 */
internal suspend fun <Request> postData(
    context: Context,
    path: String,
    data: Request,
    requestSerializer: KSerializer<Request>
) {
    postJsonWithRetries(
        context = context,
        path = path,
        jsonData = Config.json.encodeToString(requestSerializer, data)
    )
}

/**
 * Send a get request to a bouncer endpoint and parse the response.
 */
internal suspend fun <Response, Error> getForResult(
    context: Context,
    path: String,
    responseSerializer: KSerializer<Response>,
    errorSerializer: KSerializer<Error>
): NetworkResult<out Response, out Error> =
    translateNetworkResult(getWithRetries(context, path), responseSerializer, errorSerializer)

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
                body = Config.json.decodeFromString(responseSerializer, networkResult.body)
            )
        } catch (t: Throwable) {
            try {
                NetworkResult.Error(
                    responseCode = networkResult.responseCode,
                    error = Config.json.decodeFromString(errorSerializer, networkResult.body)
                )
            } catch (et: Throwable) {
                NetworkResult.Exception(networkResult.responseCode, t)
            }
        }
    is NetworkResult.Error ->
        try {
            NetworkResult.Error(
                responseCode = networkResult.responseCode,
                error = Config.json.decodeFromString(errorSerializer, networkResult.error)
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
 * Send a post request to a bouncer endpoint with retries.
 */
private suspend fun postJsonWithRetries(
    context: Context,
    path: String,
    jsonData: String
): NetworkResult<out String, out String> =
    try {
        retry(
            retryDelay = NetworkConfig.retryDelay,
            times = NetworkConfig.retryTotalAttempts
        ) {
            val result = postJson(context, path, jsonData)
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
 * Send a get request to a bouncer endpoint with retries.
 */
private suspend fun getWithRetries(
    context: Context,
    path: String,
): NetworkResult<out String, out String> =
    try {
        retry(
            retryDelay = NetworkConfig.retryDelay,
            times = NetworkConfig.retryTotalAttempts
        ) {
            val result = get(context, path)
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
 * Send a post request to a bouncer endpoint.
 */
private fun postJson(
    context: Context,
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
            setRequestHeaders(context)
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
 * Send a get request to a bouncer endpoint.
 */
private fun get(
    context: Context,
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
            setRequestHeaders(context)

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
internal suspend fun downloadFileWithRetries(context: Context, url: URL, outputFile: File) = retry(
    NetworkConfig.retryDelay,
    excluding = listOf(FileNotFoundException::class.java)
) {
    downloadFile(context, url, outputFile)
}

/**
 * Download a file.
 */
@Throws(IOException::class)
private fun downloadFile(
    context: Context,
    url: URL,
    outputFile: File,
) = networkTimer.measure(url.toString()) {
    try {
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = REQUEST_METHOD_GET

            // Set the connection to only receive data
            doOutput = false
            doInput = true

            // set headers
            setRequestHeaders(context)

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
private fun HttpURLConnection.setRequestHeaders(context: Context) {
    setRequestProperty(REQUEST_PROPERTY_AUTHENTICATION, Config.apiKey)
    setRequestProperty(REQUEST_PROPERTY_USER_AGENT, buildUserAgent(context))
    setRequestProperty(REQUEST_PROPERTY_DEVICE_ID, buildDeviceId(context))
}

@Serializable
private data class DeviceIdStructure(
    /**
     * android_id
     */
    val a: String,

    /**
     * vendor_id
     */
    val v: String,

    /**
     * advertising_id
     */
    val d: String
)

private val buildDeviceId = cacheFirstResult { context: Context ->
    DeviceIds.fromContext(context).run {
        Base64.encodeToString(
            Config.json.encodeToString(
                DeviceIdStructure.serializer(),
                DeviceIdStructure(a = androidId ?: "", v = "", d = "")
            ).toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE
        )
    }
}

private val buildUserAgent = cacheFirstResult { context: Context ->
    "cardverificationsheet/${getPlatform()}/${getAppPackageName(context)}/${getDeviceName()}/" +
        "${getOsVersion()}/${getSdkVersion()}/${getSdkFlavor()}"
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
