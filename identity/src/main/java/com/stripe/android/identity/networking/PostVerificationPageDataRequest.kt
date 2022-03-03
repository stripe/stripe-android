package com.stripe.android.identity.networking

import com.stripe.android.core.networking.DEFAULT_RETRY_CODES
import com.stripe.android.core.networking.StripeRequest
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * POST request for https://api.stripe.com/v1/identity/verification_pages/:id/data
 */
internal class PostVerificationPageDataRequest(
    id: String,
    ephemeralKey: String,
    val encodedData: String
) : StripeRequest() {
    override val method = Method.POST

    override val mimeType = MimeType.Form

    override val retryResponseCodes = DEFAULT_RETRY_CODES

    override val url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$id/$DATA"

    override val headers = IdentityHeaderFactory.createHeaderWithEphemeralKey(ephemeralKey)

    override var postHeaders: Map<String, String>? = IdentityHeaderFactory.createPostHeader()

    override fun writePostBody(outputStream: OutputStream) {
        OutputStreamWriter(outputStream).use {
            it.write(encodedData)
            it.flush()
        }
    }

    internal companion object {
        const val DATA = "data"
    }
}
