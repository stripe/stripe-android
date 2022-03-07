package com.stripe.android.identity.networking

import com.stripe.android.core.networking.DEFAULT_RETRY_CODES
import com.stripe.android.core.networking.StripeRequest

/**
 * A request to download ML models.
 */
internal class IdentityModelDownloadRequest(
    override val url: String
) : StripeRequest() {
    override val method: Method = Method.GET
    override val mimeType: MimeType = MimeType.Form
    override val retryResponseCodes = DEFAULT_RETRY_CODES
    override val headers: Map<String, String> = mapOf()
}
