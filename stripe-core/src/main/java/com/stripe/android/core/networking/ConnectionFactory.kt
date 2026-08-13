package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.InvalidRequestException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Factory to create [StripeConnection], which encapsulates an [OkHttpClient] call, triggers the
 * request and parses the response with different body type as [StripeResponse].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ConnectionFactory {
    /**
     * Creates a [StripeConnection] which attempts to parse the http response body as a [String].
     */
    @Throws(IOException::class, InvalidRequestException::class)
    fun create(request: StripeRequest): StripeConnection<String>

    /**
     * Creates a [StripeConnection] which attempts to parse the http response body as a [File].
     */
    @Throws(IOException::class, InvalidRequestException::class)
    fun createForFile(request: StripeRequest, outputFile: File): StripeConnection<File>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Default : ConnectionFactory {
        @Volatile
        var okHttpClient: OkHttpClient = buildDefaultClient()

        fun buildDefaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()

        @Throws(IOException::class, InvalidRequestException::class)
        override fun create(request: StripeRequest): StripeConnection<String> {
            val response = okHttpClient.newCall(buildOkHttpRequest(request)).execute()
            return StripeConnection.Default(response)
        }

        override fun createForFile(
            request: StripeRequest,
            outputFile: File
        ): StripeConnection<File> {
            val response = okHttpClient.newCall(buildOkHttpRequest(request)).execute()
            return StripeConnection.FileConnection(response, outputFile)
        }

        private fun buildOkHttpRequest(request: StripeRequest): Request {
            val builder = Request.Builder().url(request.url)

            request.headers.forEach { (key, value) ->
                builder.header(key, value)
            }

            when (request.method) {
                StripeRequest.Method.GET -> builder.get()
                StripeRequest.Method.DELETE -> builder.delete()
                StripeRequest.Method.POST -> {
                    val contentType = (
                        request.postHeaders?.get(HEADER_CONTENT_TYPE)
                            ?: request.mimeType.code
                        ).toMediaType()

                    request.postHeaders?.forEach { (key, value) ->
                        if (!key.equals(HEADER_CONTENT_TYPE, ignoreCase = true)) {
                            builder.header(key, value)
                        }
                    }

                    val bodyBytes = ByteArrayOutputStream().also { outputStream ->
                        request.writePostBody(outputStream)
                    }.toByteArray()

                    builder.post(bodyBytes.toRequestBody(contentType))
                }
            }

            return builder.build()
        }
    }

    private companion object {
        private val CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(30)
        private val READ_TIMEOUT = TimeUnit.SECONDS.toMillis(80)
        private const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
