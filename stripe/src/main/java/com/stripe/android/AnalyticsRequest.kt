package com.stripe.android

internal object AnalyticsRequest {
    const val HOST = "https://q.stripe.com"

    @JvmSynthetic
    internal fun create(
        params: Map<String, *>,
        requestOptions: ApiRequest.Options,
        appInfo: AppInfo? = null
    ): ApiRequest {
        return ApiRequest.createGet(HOST, requestOptions, params, appInfo)
    }
}
