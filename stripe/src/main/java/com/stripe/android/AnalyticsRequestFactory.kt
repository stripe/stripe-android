package com.stripe.android

internal class AnalyticsRequestFactory(
    private val logger: Logger = Logger.noop()
) {
    @JvmSynthetic
    internal fun create(
        params: Map<String, *>,
        requestOptions: ApiRequest.Options,
        appInfo: AppInfo? = null
    ): ApiRequest {
        logger.info("Event: ${params[AnalyticsDataFactory.FIELD_EVENT]}")
        return ApiRequest.Factory(appInfo = appInfo)
            .createGet(HOST, requestOptions, params)
    }

    internal companion object {
        internal const val HOST = "https://q.stripe.com"
    }
}
