package com.stripe.android.crypto.onramp.repositories

import android.webkit.MimeTypeMap
import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.FileUploadRequest
import java.io.File
import java.io.OutputStream
import java.io.PrintWriter
import java.net.URLConnection
import java.util.Locale

internal class OnrampFileUploadRequest(
    private val file: File,
    options: ApiRequest.Options,
    appInfo: AppInfo?,
) : FileUploadRequest(
    fileParams = StripeFileParams(file, StripeFilePurpose.CryptoOnrampKycDocument),
    options = options,
    appInfo = appInfo,
) {
    override fun writePostBody(outputStream: OutputStream) {
        val contentType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase(Locale.ROOT))
            ?: URLConnection.guessContentTypeFromName(file.name)
            ?: "application/octet-stream"
        val metadata = """
            --$boundary
            Content-Disposition: form-data; name="file"; filename="${file.name}"
            Content-Type: $contentType
            Content-Transfer-Encoding: binary


        """.trimIndent()

        outputStream.writer().use { streamWriter ->
            PrintWriter(streamWriter, true).use { writer ->
                writeString(writer, purposeContents)
                writeString(writer, metadata)
                writeFile(outputStream)
                writer.write(LINE_BREAK)
                writer.write("--$boundary--")
                writer.flush()
            }
        }
    }
}
