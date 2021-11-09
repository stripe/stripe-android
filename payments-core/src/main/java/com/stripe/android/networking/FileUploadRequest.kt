package com.stripe.android.networking

import androidx.annotation.VisibleForTesting
import com.stripe.android.AppInfo
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeRequest.MimeType
import com.stripe.android.model.StripeFileParams
import java.io.OutputStream
import java.io.PrintWriter
import java.net.URLConnection
import kotlin.random.Random

/**
 * A [StripeRequest] for uploading a file using [MimeType.MultipartForm].
 *
 * See [File upload guide](https://stripe.com/docs/file-upload)
 */
internal class FileUploadRequest(
    private val fileParams: StripeFileParams,
    options: ApiRequest.Options,
    appInfo: AppInfo? = null,
    /**
     * Boundary to delineate parts of the message
     *
     * See [Multipart messages](https://en.wikipedia.org/wiki/MIME#Multipart_messages)
     */
    private val boundary: String = createBoundary()
) : StripeRequest() {
    private val headersFactory: RequestHeadersFactory = RequestHeadersFactory.FileUpload(
        options = options,
        appInfo = appInfo,
        boundary = boundary
    )

    override val method: Method = Method.POST

    override val mimeType: MimeType = MimeType.MultipartForm

    override val url = HOST

    override val retryResponseCodes: Iterable<Int> = PAYMENT_RETRY_CODES

    override val headers: Map<String, String> = headersFactory.create()

    override var postHeaders: Map<String, String>? = headersFactory.createPostHeader()

    override fun writePostBody(outputStream: OutputStream) {
        outputStream.writer().use {
            PrintWriter(it, true).use { writer ->
                writeString(writer, purposeContents)
                writeString(writer, fileMetadata)
                writeFile(outputStream)

                writer.write(LINE_BREAK)
                writer.write("--$boundary--")
                writer.flush()
            }
        }
    }

    private fun writeString(writer: PrintWriter, contents: String) {
        writer.write(contents.replace("\n", LINE_BREAK))
        writer.flush()
    }

    private fun writeFile(outputStream: OutputStream) {
        fileParams.file.inputStream().copyTo(outputStream)
    }

    @VisibleForTesting
    internal val fileMetadata: String
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

    @VisibleForTesting
    internal val purposeContents: String
        get() {
            return """
                --$boundary
                Content-Disposition: form-data; name="purpose"

                ${fileParams.purpose.code}

            """.trimIndent()
        }

    internal companion object {
        private const val LINE_BREAK = "\r\n"

        private const val HOST = "https://files.stripe.com/v1/files"

        private fun createBoundary(): String {
            return Random.Default.nextLong(0, Long.MAX_VALUE).toString()
        }
    }
}
