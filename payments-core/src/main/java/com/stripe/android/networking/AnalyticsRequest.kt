package com.stripe.android.networking

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsRequest(
    val params: Map<String, *>
) : StripeRequest() {
    private val query: String = QueryStringFactory.createFromParamsWithEmptyValues(params)

    override val method: Method = Method.GET

    override val mimeType: MimeType = MimeType.Form

    override val retryResponseCodes: Iterable<Int> = PAYMENT_RETRY_CODES

    override val url = listOfNotNull(
        HOST,
        query.takeIf { it.isNotEmpty() }
    ).joinToString("?")

    override val headers = RequestHeadersFactory.Analytics.create()

    internal companion object {
        internal const val HOST = "https://q.stripe.com"
    }
}
