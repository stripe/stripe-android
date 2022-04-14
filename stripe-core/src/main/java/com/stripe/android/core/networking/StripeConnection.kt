package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Scanner

/**
 * A wrapper for accessing a [HttpURLConnection]. Implements [Closeable] to simplify closing related
 * resources.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeConnection<ResponseBodyType> : Closeable {
    val responseCode: Int
    val response: StripeResponse<ResponseBodyType>
    fun createBodyFromResponseStream(responseStream: InputStream?): ResponseBodyType?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract class AbstractConnection<ResponseBodyType>(
        private val conn: HttpURLConnection
    ) : StripeConnection<ResponseBodyType> {
        override val responseCode: Int
            @JvmSynthetic
            get() {
                return conn.responseCode
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
                    headers = conn.headerFields
                )
            }

        private val responseStream: InputStream?
            @Throws(IOException::class)
            get() {
                return if (responseCode in 200..299) {
                    conn.inputStream
                } else {
                    conn.errorStream
                }
            }

        override fun close() {
            responseStream?.close()
            conn.disconnect()
        }

        internal companion object {
            internal val CHARSET = StandardCharsets.UTF_8.name()
        }
    }

    /**
     * Default [StripeConnection] that converts the ResponseStream to a String.
     */
    class Default internal constructor(
        conn: HttpURLConnection
    ) : AbstractConnection<String>(conn = conn) {

        /**
         * Convert stream to a String
         */
        @Throws(IOException::class)
        override fun createBodyFromResponseStream(responseStream: InputStream?): String? {
            if (responseStream == null) {
                return null
            }

            responseStream.use {
                // \A is the beginning of the stream boundary
                val scanner = Scanner(responseStream, CHARSET).useDelimiter("\\A")
                return if (scanner.hasNext()) {
                    scanner.next()
                } else {
                    null
                }
            }
        }
    }

    /**
     * [StripeConnection] that writes the ResponseStream to a File.
     */
    class FileConnection internal constructor(
        conn: HttpURLConnection,
        private val outputFile: File
    ) : AbstractConnection<File>(conn = conn) {

        /**
         * Convert stream to a File
         */
        @Throws(IOException::class)
        override fun createBodyFromResponseStream(responseStream: InputStream?): File? {
            if (responseStream == null) {
                return null
            }
            responseStream.use { stream ->
                FileOutputStream(outputFile).use { stream.copyTo(it) }
            }
            return outputFile
        }
    }
}
