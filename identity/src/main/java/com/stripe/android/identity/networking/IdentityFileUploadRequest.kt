package com.stripe.android.identity.networking

import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.FileUploadRequest
import okio.BufferedSink

/**
 * Identity's [FileUploadRequest] that needs to add form data "owned_by"
 */
internal class IdentityFileUploadRequest(
    fileParams: StripeFileParams,
    options: ApiRequest.Options,
    appInfo: AppInfo? = null,
    internal val verificationId: String
) : FileUploadRequest(fileParams, options, appInfo) {

    override fun writePostBody(sink: BufferedSink) {
        writeString(sink, ownedBy)
        writeString(sink, purposeContents)
        writeString(sink, fileMetadata)
        writeFile(sink)

        sink.writeUtf8(LINE_BREAK)
        sink.writeUtf8("--$boundary--")
        sink.flush()
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
