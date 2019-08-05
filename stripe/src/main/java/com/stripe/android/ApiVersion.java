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
    @NonNull private static final String API_VERSION_CODE = "2019-05-16";

    @NonNull private static final ApiVersion INSTANCE = new ApiVersion(API_VERSION_CODE);

    @NonNull public final String code;

    @NonNull
    static ApiVersion get() {
        return INSTANCE;
    }

    private ApiVersion(@NonNull String code) {
        this.code = code;
    }

    @NonNull
    @Override
    public String toString() {
        return code;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(code);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof ApiVersion && typedEquals((ApiVersion) obj));
    }

    private boolean typedEquals(@NonNull ApiVersion apiVersion) {
        return ObjectUtils.equals(code, apiVersion.code);
    }
}
