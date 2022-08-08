package com.stripe.android.core.networking

import androidx.annotation.RestrictTo

/**
 * Analytics request sent to q.stripe.com, which is a legacy analytics service used mostly by
 * Payment SDK, analytics are saved in a shared DB table with payment-specific schema.
 *
 * For other SDKs, it is recommended to create a dedicated DB table just for the SDK and write to
 * this table through r.stripe.com. See [AnalyticsRequestV2] for details.
 */
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
