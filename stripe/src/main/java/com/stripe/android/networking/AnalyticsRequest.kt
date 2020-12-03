package com.stripe.android.networking

import com.stripe.android.Logger

internal data class AnalyticsRequest(
    override val params: Map<String, *>
) : StripeRequest() {
    override val method: Method = Method.GET
    override val baseUrl: String = HOST
    override val mimeType: MimeType = MimeType.Form
    override val headersFactory: RequestHeadersFactory = RequestHeadersFactory.Analytics

    class Factory(
        private val logger: Logger = Logger.noop()
    ) {
        @JvmSynthetic
        internal fun create(
            params: Map<String, *>
        ): AnalyticsRequest {
            logger.info("Event: ${params[AnalyticsDataFactory.FIELD_EVENT]}")

            return AnalyticsRequest(
                params
            )
        }
    }

    internal companion object {
        internal const val HOST = "https://q.stripe.com"
    }
}
