package com.stripe.android

import com.stripe.android.model.StripeJsonUtils

/**
 * A class representing a fingerprint request.
 */
internal class FingerprintRequest(
    override val params: Map<String, Any>,
    private val guid: String
) : StripeRequest() {
    override val method: Method = Method.POST
    override val baseUrl: String = URL
    override val mimeType: MimeType = MimeType.Json
    override val userAgent: String = DEFAULT_USER_AGENT

    override val body: String
        get() {
            return StripeJsonUtils.mapToJsonObject(params).toString()
        }

    override fun createHeaders(): Map<String, String> {
        return mapOf("Cookie" to "m=$guid")
    }

    private companion object {
        private const val URL = "https://m.stripe.com/4"
    }
}
