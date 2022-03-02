package com.stripe.android.identity.networking

import com.stripe.android.core.networking.DEFAULT_RETRY_CODES
import com.stripe.android.core.networking.StripeRequest

/**
 * Get request for https://api.stripe.com/v1/identity/verification_pages/:id
 */
internal class RetrieveVerificationPageRequest(
    id: String,
    ephemeralKey: String
) : StripeRequest() {
    override val method = Method.GET

    override val mimeType = MimeType.Form

    override val retryResponseCodes = DEFAULT_RETRY_CODES

    override val url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/$id"

    override val headers = IdentityHeaderFactory.createHeaderWithEphemeralKey(ephemeralKey)
}
