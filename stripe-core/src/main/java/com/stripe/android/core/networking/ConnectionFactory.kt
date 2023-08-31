package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.InvalidRequestException
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Factory to create [StripeConnection], which encapsulates an [HttpURLConnection], triggers the
 * request and parses the response with different body type as [StripeResponse].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ConnectionFactory {
    /**
     * Creates an [StripeConnection] which attempts to parse the http response body as a [String].
     */
    @Throws(IOException::class, InvalidRequestException::class)
    fun create(request: StripeRequest): StripeConnection<String>

    /**
     * Creates an [StripeConnection] which attempts to parse the http response body as a [File].
     */
    @Throws(IOException::class, InvalidRequestException::class)
    fun createForFile(request: StripeRequest, outputFile: File): StripeConnection<File>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface ConnectionOpener {
        fun open(
            request: StripeRequest,
            callback: HttpURLConnection.(request: StripeRequest) -> Unit
        ): HttpURLConnection

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object Default : ConnectionOpener {
            override fun open(
                request: StripeRequest,
                callback: HttpURLConnection.(request: StripeRequest) -> Unit
            ): HttpURLConnection {
                return (URL(request.url).openConnection() as HttpURLConnection).apply {
                    callback(request)
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Default : ConnectionFactory {
        @Volatile
        var connectionOpener: ConnectionOpener = ConnectionOpener.Default

        @Throws(IOException::class, InvalidRequestException::class)
        @JvmSynthetic
        override fun create(request: StripeRequest): StripeConnection<String> {
            return StripeConnection.Default(openConnectionAndApplyFields(request))
        }

        override fun createForFile(
            request: StripeRequest,
            outputFile: File
        ): StripeConnection<File> {
            return StripeConnection.FileConnection(
                openConnectionAndApplyFields(request),
                outputFile
            )
        }

        private fun openConnectionAndApplyFields(
            originalRequest: StripeRequest
        ): HttpURLConnection {
            return connectionOpener.open(originalRequest) { request ->
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                useCaches = request.shouldCache
                requestMethod = request.method.code

                request.headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }

                if (StripeRequest.Method.POST == request.method) {
                    doOutput = true
                    request.postHeaders?.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                    outputStream.use { output -> request.writePostBody(output) }
                }
            }
        }
    }

    private companion object {
        private val CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(30).toInt()
        private val READ_TIMEOUT = TimeUnit.SECONDS.toMillis(80).toInt()
    }
}
