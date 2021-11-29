package com.stripe.android.core.networking

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsRequest(
    val params: Map<String, *>,
    override val headers: Map<String, String>
) : StripeRequest() {
    private val query: String = QueryStringFactory.createFromParamsWithEmptyValues(params)

    override val method: Method = Method.GET

    override val mimeType: MimeType = MimeType.Form

    override val retryResponseCodes: Iterable<Int> = HTTP_TOO_MANY_REQUESTS..HTTP_TOO_MANY_REQUESTS

    override val url = listOfNotNull(
        HOST,
        query.takeIf { it.isNotEmpty() }
    ).joinToString("?")

    internal companion object {
        internal const val HOST = "https://q.stripe.com"
    }
}
