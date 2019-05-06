package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import java.util.Map;

class StripeRequest {
    @NonNull final Method method;
    @NonNull final String url;
    @Nullable final Map<String, ?> params;
    @NonNull final RequestOptions options;

    @NonNull
    static StripeRequest createGet(@NonNull String url,
                                   @NonNull RequestOptions options) {
        return new StripeRequest(Method.GET, url, null, options);
    }

    @NonNull
    static StripeRequest createGet(@NonNull String url,
                                   @NonNull Map<String, ?> params,
                                   @NonNull RequestOptions options) {
        return new StripeRequest(Method.GET, url, params, options);
    }

    @NonNull
    static StripeRequest createPost(@NonNull String url,
                                    @NonNull RequestOptions options) {
        return new StripeRequest(Method.POST, url, null, options);
    }

    @NonNull
    static StripeRequest createPost(@NonNull String url,
                                    @NonNull Map<String, ?> params,
                                    @NonNull RequestOptions options) {
        return new StripeRequest(Method.POST, url, params, options);
    }

    @NonNull
    static StripeRequest createDelete(@NonNull String url,
                                      @NonNull RequestOptions options) {
        return new StripeRequest(Method.DELETE, url, null, options);
    }

    private StripeRequest(@NonNull Method method,
                          @NonNull String url,
                          @Nullable Map<String, ?> params,
                          @NonNull RequestOptions options) {
        this.method = method;
        this.url = url;
        this.params = params;
        this.options = options;
    }
    
    @Override
    public int hashCode() {
        return ObjectUtils.hash(method, url, params, options);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj) ||
                (obj instanceof StripeRequest && typedEquals((StripeRequest) obj));
    }

    private boolean typedEquals(@NonNull StripeRequest request) {
        return ObjectUtils.equals(method, request.method) &&
                ObjectUtils.equals(url, request.url) &&
                ObjectUtils.equals(params, request.params) &&
                ObjectUtils.equals(options, request.options);
    }

    enum Method {
        GET("GET"),
        POST("POST"),
        DELETE("DELETE");

        @NonNull final String code;

        Method(@NonNull String code) {
            this.code = code;
        }
    }
}
