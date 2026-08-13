package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Scanner
import okhttp3.Response

/**
 * A wrapper around an OkHttp [Response]. Implements [Closeable] to simplify closing related
 * resources.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeConnection<ResponseBodyType> : Closeable {
    val responseCode: Int
    val response: StripeResponse<ResponseBodyType>
    fun createBodyFromResponseStream(responseStream: InputStream?): ResponseBodyType?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    abstract class AbstractConnection<ResponseBodyType>(
        private val okHttpResponse: Response
    ) : StripeConnection<ResponseBodyType> {
        override val responseCode: Int
            @JvmSynthetic
            get() = okHttpResponse.code

        override val response: StripeResponse<ResponseBodyType>
            @Throws(IOException::class)
            @JvmSynthetic
            get() = StripeResponse(
                code = responseCode,
                body = createBodyFromResponseStream(okHttpResponse.body?.byteStream()),
                headers = okHttpResponse.headers.toMultimap()
            )

        override fun close() {
            okHttpResponse.close()
        }

        internal companion object {
            internal val CHARSET = StandardCharsets.UTF_8.name()
        }
    }

    /**
     * Default [StripeConnection] that converts the response body to a [String].
     */
    class Default internal constructor(
        okHttpResponse: Response
    ) : AbstractConnection<String>(okHttpResponse = okHttpResponse) {

        /**
         * Convert stream to a String.
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
     * [StripeConnection] that writes the response body to a [File].
     */
    class FileConnection internal constructor(
        okHttpResponse: Response,
        private val outputFile: File
    ) : AbstractConnection<File>(okHttpResponse = okHttpResponse) {

        /**
         * Convert stream to a File.
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
