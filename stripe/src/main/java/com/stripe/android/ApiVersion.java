package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

/**
 * A class that represents a Stripe API version.
 *
 * See <a href="https://stripe.com/docs/api/versioning">https://stripe.com/docs/api/versioning</a>
 * for documentation on API versioning.
 *
 * See <a href="https://stripe.com/docs/upgrades">https://stripe.com/docs/upgrades</a> for latest
 * API changes.
 */
final class ApiVersion {
    static final String DEFAULT_API_VERSION = "2019-05-16";

    @NonNull private static final ApiVersion DEFAULT_INSTANCE = new ApiVersion(DEFAULT_API_VERSION);

    @NonNull private final String mCode;

    @NonNull
    static ApiVersion create(@NonNull String code) {
        return new ApiVersion(code);
    }

    @NonNull
    static ApiVersion getDefault() {
        return DEFAULT_INSTANCE;
    }

    private ApiVersion(@NonNull String code) {
        this.mCode = code;
    }

    @NonNull
    String getCode() {
        return mCode;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mCode);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof ApiVersion && typedEquals((ApiVersion) obj));
    }

    private boolean typedEquals(@NonNull ApiVersion apiVersion) {
        return ObjectUtils.equals(mCode, apiVersion.mCode);
    }
}
