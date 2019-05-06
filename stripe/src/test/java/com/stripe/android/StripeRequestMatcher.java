package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mockito.ArgumentMatcher;

import java.util.Map;

public class StripeRequestMatcher implements ArgumentMatcher<StripeRequest> {
    @NonNull private final StripeRequest.Method mMethod;
    @NonNull private final String mUrl;
    @Nullable private final Map<String, ?> mParams;
    @Nullable private final RequestOptions mOptions;

    StripeRequestMatcher(@NonNull StripeRequest.Method method,
                         @NonNull String url) {
        this(method, url, null);
    }

    StripeRequestMatcher(@NonNull StripeRequest.Method method,
                         @NonNull String url,
                         @Nullable Map<String, ?> params) {
        this(method, url, params, null);
    }

    private StripeRequestMatcher(@NonNull StripeRequest.Method method,
                                 @NonNull String url,
                                 @Nullable Map<String, ?> params,
                                 @Nullable RequestOptions options) {
        mMethod = method;
        mUrl = url;
        mParams = params;
        mOptions = options;
    }

    @Override
    public boolean matches(@NonNull StripeRequest request) {
        return mMethod == request.method &&
                mUrl.equals(request.url) &&
                (mParams == null || mParams.equals(request.params)) &&
                (mOptions == null || mOptions.equals(request.options));
    }
}
