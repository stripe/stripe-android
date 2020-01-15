package com.stripe.android

import androidx.annotation.VisibleForTesting
import com.stripe.android.StripeRequest.MimeType
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
    systemPropertySupplier: (String) -> String = DEFAULT_SYSTEM_PROPERTY_SUPPLIER,

    /**
     * Boundary to delineate parts of the message
     *
     * See [Multipart messages](https://en.wikipedia.org/wiki/MIME#Multipart_messages)
     */
    private val boundary: String = createBoundary()
) : StripeRequest() {

    override val method: Method = Method.POST
    override val baseUrl: String = "https://files.stripe.com/v1/files"
    override val params: Map<String, *>? = null
    override val mimeType: MimeType = MimeType.MultipartForm
    override val headersFactory: RequestHeadersFactory = RequestHeadersFactory.Api(
        options = options,
        appInfo = appInfo,
        systemPropertySupplier = systemPropertySupplier
    )

    override fun writeBody(outputStream: OutputStream) {
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

    override val contentType: String
        get() {
            return "${mimeType.code}; boundary=$boundary"
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

        private fun createBoundary(): String {
            return Random.Default.nextLong(0, Long.MAX_VALUE).toString()
        }
    }
}
