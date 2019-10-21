package com.stripe.android

internal object AnalyticsRequest {
    const val HOST = "https://q.stripe.com"

    @JvmSynthetic
    @JvmOverloads
    internal fun create(
        params: Map<String, *>,
        requestOptions: ApiRequest.Options,
        appInfo: AppInfo? = null
    ): ApiRequest {
        return ApiRequest(StripeRequest.Method.GET, HOST, params, requestOptions, appInfo)
    }
}
