package com.stripe.android.networking

data class AnalyticsRequest(
    override val params: Map<String, *>
) : StripeRequest() {
    override val method: Method = Method.GET
    override val baseUrl: String = HOST
    override val mimeType: MimeType = MimeType.Form
    override val headersFactory: RequestHeadersFactory = RequestHeadersFactory.Analytics

    internal companion object {
        internal const val HOST = "https://q.stripe.com"
    }
}
