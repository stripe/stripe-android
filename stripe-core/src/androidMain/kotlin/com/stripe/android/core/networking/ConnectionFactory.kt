package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.InvalidRequestException
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.IOException
import okio.Path
import kotlin.time.Duration.Companion.seconds

/**
 * Factory to create [StripeConnection], which encapsulates an [ktor client], triggers the
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
     * Creates a [StripeConnection] which attempts to parse the http response body into a file [Path].
     */
    @Throws(IOException::class, InvalidRequestException::class)
    fun createForFile(request: StripeRequest, outputFile: Path): StripeConnection<Path>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface ConnectionOpener {
        fun open(
            request: StripeRequest,
            callback: HttpClient.(request: StripeRequest) -> StripeKtorConnection
        ): StripeKtorConnection

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object Default : ConnectionOpener {
            override fun open(
                request: StripeRequest,
                callback: HttpClient.(request: StripeRequest) -> StripeKtorConnection
            ): StripeKtorConnection {
                return callback(createHttpClient(), request)
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
            return StripeConnection.Default(
                connectionOpener.open(request) { originalRequest ->
                    startHttpRequest(this, originalRequest)
                }
            )
        }

        override fun createForFile(
            request: StripeRequest,
            outputFile: Path
        ): StripeConnection<Path> {
            return StripeConnection.FileConnection(
                connectionOpener.open(request) { originalRequest ->
                    startHttpRequest(this, originalRequest)
                },
                outputFile
            )
        }

        private fun startHttpRequest(
            client: HttpClient,
            originalRequest: StripeRequest,
        ): StripeKtorConnection {
            val httpResponse = runBlocking {
                when (originalRequest.method) {
                    StripeRequest.Method.GET -> {
                        client.get(originalRequest.url) {
                            originalRequest.headers.forEach { (key, value) ->
                                headers.append(key, value)
                            }
                        }
                    }
                    StripeRequest.Method.POST -> {
                        val response = client.post(originalRequest.url) {
                            originalRequest.headers.forEach { (key, value) ->
                                headers.append(key, value)
                            }

                            originalRequest.postHeaders?.forEach { (key, value) ->
                                headers.append(key, value)
                            }

                            val bodyBuffer = Buffer()
                            originalRequest.writePostBody(bodyBuffer)
                            setBody(bodyBuffer.readByteArray())
                        }
                        response
                    }
                    StripeRequest.Method.DELETE -> {
                        client.delete(originalRequest.url) {
                            originalRequest.headers.forEach { (key, value) ->
                                headers.append(key, value)
                            }
                        }
                    }
                }
            }
            return StripeKtorConnection(client, httpResponse)
        }

    }

    private companion object {
        private val CONNECT_TIMEOUT = 30.seconds.inWholeMilliseconds
        private val READ_TIMEOUT = 80.seconds.inWholeMilliseconds

        private fun createHttpClient(): HttpClient {
            return HttpClient(OkHttp) {

                // Timeouts
                install(HttpTimeout) {
                    // No equivalent in HttpUrlConnection
//                    requestTimeoutMillis = 15_000
                    connectTimeoutMillis = CONNECT_TIMEOUT
                    socketTimeoutMillis = READ_TIMEOUT
                }
                install(HttpCache)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class StripeKtorConnection(
        val client: HttpClient,
        val httpResponse: HttpResponse
    )
}
