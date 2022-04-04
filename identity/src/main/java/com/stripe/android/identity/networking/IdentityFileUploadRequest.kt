package com.stripe.android.identity.networking

import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.FileUploadRequest
import java.io.OutputStream
import java.io.PrintWriter

/**
 * Identity's [FileUploadRequest] that needs to add form data "owned_by"
 */
internal class IdentityFileUploadRequest(
    fileParams: StripeFileParams,
    options: ApiRequest.Options,
    appInfo: AppInfo? = null,
    internal val verificationId: String
) : FileUploadRequest(fileParams, options, appInfo) {

    override fun writePostBody(outputStream: OutputStream) {
        outputStream.writer().use {
            PrintWriter(it, true).use { writer ->
                writeString(writer, ownedBy)
                writeString(writer, purposeContents)
                writeString(writer, fileMetadata)
                writeFile(outputStream)

                writer.write(LINE_BREAK)
                writer.write("--$boundary--")
                writer.flush()
            }
        }
    }

    private val ownedBy: String
        get() {
            return """
                --$boundary
                Content-Disposition: form-data; name="owned_by"

                $verificationId

            """.trimIndent()
        }
}
