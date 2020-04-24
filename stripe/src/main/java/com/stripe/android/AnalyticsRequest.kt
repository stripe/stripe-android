package com.stripe.android

internal data class AnalyticsRequest(
    override val params: Map<String, *>,
    internal val options: ApiRequest.Options,
    internal val appInfo: AppInfo? = null
) : StripeRequest() {
    override val method: Method = Method.GET
    override val baseUrl: String = HOST
    override val mimeType: MimeType = MimeType.Form
    override val headersFactory: RequestHeadersFactory = RequestHeadersFactory.Api(
        options = options,
        appInfo = appInfo
    )

    class Factory(
        private val logger: Logger = Logger.noop()
    ) {
        @JvmSynthetic
        internal fun create(
            params: Map<String, *>,
            options: ApiRequest.Options,
            appInfo: AppInfo? = null
        ): AnalyticsRequest {
            logger.info("Event: ${params[AnalyticsDataFactory.FIELD_EVENT]}")

            return AnalyticsRequest(
                params,
                options,
                appInfo
            )
        }
    }

    internal companion object {
        internal const val HOST = "https://q.stripe.com"
    }
}
