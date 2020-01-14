package com.stripe.android

import com.stripe.android.model.StripeJsonUtils

/**
 * A class representing a fingerprint request.
 */
internal class FingerprintRequest(
    override val params: Map<String, Any>,
    guid: String
) : StripeRequest() {
    override val method: Method = Method.POST
    override val baseUrl: String = URL
    override val mimeType: MimeType = MimeType.Json
    override val headersFactory: RequestHeadersFactory = RequestHeadersFactory.Default(
        mapOf("Cookie" to "m=$guid")
    )

    override val body: String
        get() {
            return StripeJsonUtils.mapToJsonObject(params).toString()
        }

    private companion object {
        private const val URL = "https://m.stripe.com/4"
    }
}
