package com.stripe.android.core.networking

import com.stripe.android.core.exception.InvalidRequestException
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

/**
 * Factory to create [StripeConnection], which encapsulates an [HttpsURLConnection], triggers the
 * request and parses the response with different body type as [StripeResponse].
 */
internal interface ConnectionFactory {
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

    object Default : ConnectionFactory {
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

        private fun openConnectionAndApplyFields(request: StripeRequest): HttpsURLConnection {
            return (URL(request.url).openConnection() as HttpsURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                useCaches = false
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
