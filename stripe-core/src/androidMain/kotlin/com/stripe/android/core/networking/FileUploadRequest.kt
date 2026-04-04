package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.networking.StripeRequest.MimeType
import java.net.URLConnection
import kotlin.random.Random
import okio.BufferedSink
import okio.source

/**
 * A [StripeRequest] for uploading a file using [MimeType.MultipartForm].
 *
 * See [File upload guide](https://stripe.com/docs/file-upload)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class FileUploadRequest(
    private val fileParams: StripeFileParams,
    options: ApiRequest.Options,
    appInfo: AppInfo? = null,
    /**
     * Boundary to delineate parts of the message
     *
     * See [Multipart messages](https://en.wikipedia.org/wiki/MIME#Multipart_messages)
     */
    protected val boundary: String = createBoundary()
) : StripeRequest() {
    private val headersFactory: RequestHeadersFactory = RequestHeadersFactory.FileUpload(
        options = options,
        appInfo = appInfo,
        boundary = boundary
    )

    override val method: Method = Method.POST

    override val mimeType: MimeType = MimeType.MultipartForm

    override val url = HOST

    override val retryResponseCodes: Iterable<Int> = DEFAULT_RETRY_CODES

    override val headers: Map<String, String> = headersFactory.create()

    override var postHeaders: Map<String, String>? = headersFactory.createPostHeader()

    override fun writePostBody(sink: BufferedSink) {
        writeString(sink, purposeContents)
        writeString(sink, fileMetadata)
        writeFile(sink)

        sink.writeUtf8(LINE_BREAK)
        sink.writeUtf8("--$boundary--")
        sink.flush()
    }

    protected fun writeString(sink: BufferedSink, contents: String) {
        sink.writeUtf8(contents.replace("\n", LINE_BREAK))
        sink.flush()
    }

    protected fun writeFile(sink: BufferedSink) {
        fileParams.file.source().use { fileSource ->
            sink.writeAll(fileSource)
        }
        sink.flush()
    }

    val fileMetadata: String
        get() {
            val fileName = fileParams.file.name
            val probableContentType = URLConnection.guessContentTypeFromName(fileName)
            return """
                --$boundary
                Content-Disposition: form-data; name="file"; filename="$fileName"
                Content-Type: $probableContentType
                Content-Transfer-Encoding: binary


            """.trimIndent()
        }

    val purposeContents: String
        get() {
            return """
                --$boundary
                Content-Disposition: form-data; name="purpose"

                ${fileParams.purpose.code}

            """.trimIndent()
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected companion object {
        const val LINE_BREAK = "\r\n"

        private const val HOST = "https://files.stripe.com/v1/files"

        private fun createBoundary(): String {
            return Random.Default.nextLong(0, Long.MAX_VALUE).toString()
        }
    }
}
