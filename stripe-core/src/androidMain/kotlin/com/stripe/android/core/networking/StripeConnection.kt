package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import io.ktor.client.statement.readRawBytes
import io.ktor.util.toMap
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.buffer
import java.nio.charset.StandardCharsets

/**
 * A wrapper for accessing an http connection. Implements [AutoCloseable] to simplify closing related
 * resources.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeConnection<ResponseBodyType> : AutoCloseable {
    val responseCode: Int
    val response: StripeResponse<ResponseBodyType>
    fun createBodyFromResponseStream(responseSource: BufferedSource?): ResponseBodyType?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract class AbstractConnection<ResponseBodyType>(
        private val connection: ConnectionFactory.StripeKtorConnection
    ) : StripeConnection<ResponseBodyType> {
        override val responseCode: Int
            @JvmSynthetic
            get() {
                return connection.httpResponse.status.value
            }

        override val response: StripeResponse<ResponseBodyType>
            @Throws(IOException::class)
            @JvmSynthetic
            get() {
                // trigger the request
                val responseCode = this.responseCode
                return StripeResponse(
                    code = responseCode,
                    body = createBodyFromResponseStream(responseStream),
                    headers = connection.httpResponse.headers.toMap()
                )
            }

        private val responseStream: Buffer
            @Throws(IOException::class)
            get() {
                return Buffer().apply {
                    runBlocking {
                        write(connection.httpResponse.readRawBytes())
                    }
                }
            }

        override fun close() {
            connection.client.close()
        }
    }

    /**
     * Default [StripeConnection] that converts the ResponseStream to a String.
     */
    class Default internal constructor(
        connection: ConnectionFactory.StripeKtorConnection
    ) : AbstractConnection<String>(connection) {

        /**
         * Convert stream to a String
         */
        @Throws(IOException::class)
        override fun createBodyFromResponseStream(responseSource: BufferedSource?): String? {
            if (responseSource == null) {
                return null
            }

            responseSource.use { bufferedSource ->
                if (bufferedSource.exhausted()) {
                    return null
                }
                return bufferedSource.readString(StandardCharsets.UTF_8)
            }
        }
    }

    /**
     * [StripeConnection] that writes the ResponseStream to a file [Path].
     */
    class FileConnection internal constructor(
        connection: ConnectionFactory.StripeKtorConnection,
        private val outputFile: Path
    ) : AbstractConnection<Path>(connection) {

        /**
         * Convert stream to a file [Path].
         */
        @Throws(IOException::class)
        override fun createBodyFromResponseStream(responseSource: BufferedSource?): Path? {
            if (responseSource == null) {
                return null
            }
            responseSource.use { source ->
                FileSystem.SYSTEM.sink(outputFile).buffer().use { fileSink ->
                    fileSink.writeAll(source)
                }
            }
            return outputFile
        }
    }
}
