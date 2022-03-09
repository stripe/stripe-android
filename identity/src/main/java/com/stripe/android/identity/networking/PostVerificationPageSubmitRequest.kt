package com.stripe.android.identity.networking

import com.stripe.android.core.networking.DEFAULT_RETRY_CODES
import com.stripe.android.core.networking.StripeRequest

/**
 * Post request for https://api.stripe.com/v1/identity/verification_pages/:id/submit
 */
internal class PostVerificationPageSubmitRequest(
    id: String,
    ephemeralKey: String
) : StripeRequest() {
    override val method = Method.POST

    override val mimeType = MimeType.Form

    override val retryResponseCodes = DEFAULT_RETRY_CODES

    override val url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$id/$SUBMIT"

    override val headers = IdentityHeaderFactory.createHeaderWithEphemeralKey(ephemeralKey)

    override var postHeaders: Map<String, String>? = IdentityHeaderFactory.createPostHeader()

    internal companion object {
        const val SUBMIT = "submit"
    }
}
