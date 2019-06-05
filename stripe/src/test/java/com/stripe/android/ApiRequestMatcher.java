package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.InvalidRequestException;

import org.mockito.ArgumentMatcher;

import java.io.UnsupportedEncodingException;
import java.util.Map;

class ApiRequestMatcher implements ArgumentMatcher<ApiRequest> {
    @NonNull private final StripeRequest.Method mMethod;
    @NonNull private final String mUrl;
    @NonNull private final ApiRequest.Options mOptions;
    @Nullable private final Map<String, ?> mParams;

    ApiRequestMatcher(@NonNull StripeRequest.Method method,
                      @NonNull String url,
                      @NonNull ApiRequest.Options options) {
        this(method, url, options, null);
    }

    ApiRequestMatcher(@NonNull StripeRequest.Method method,
                      @NonNull String url,
                      @NonNull ApiRequest.Options options,
                      @Nullable Map<String, ?> params) {
        mMethod = method;
        mUrl = url;
        mOptions = options;
        mParams = params;
    }

    @Override
    public boolean matches(@NonNull ApiRequest request) {
        final String url;
        try {
            url = request.getUrl();
        } catch (UnsupportedEncodingException | InvalidRequestException e) {
            return false;
        }
        return mMethod == request.method &&
                mUrl.equals(url) &&
                mOptions.equals(request.options) &&
                (mParams == null || mParams.equals(request.params));
    }
}
