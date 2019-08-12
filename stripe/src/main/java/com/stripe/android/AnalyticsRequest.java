package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;

final class AnalyticsRequest {
    static final String HOST = "https://q.stripe.com";

    @NonNull
    static ApiRequest create(@NonNull Map<String, ?> params,
                             @NonNull ApiRequest.Options requestOptions) {
        return create(params, requestOptions, null);
    }

    @NonNull
    static ApiRequest create(@NonNull Map<String, ?> params,
                             @NonNull ApiRequest.Options requestOptions,
                             @Nullable AppInfo appInfo) {
        return new ApiRequest(StripeRequest.Method.GET, HOST, params, requestOptions,
                appInfo);
    }
}
