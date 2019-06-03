package com.stripe.android;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data class representing options for a Stripe API request.
 */
final class RequestOptions {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RequestType.API, RequestType.FINGERPRINTING})
    public @interface RequestType {
        int API = 0;
        int FINGERPRINTING = 1;
    }

    @Nullable private final String mGuid;
    @Nullable private final String mApiKey;
    @RequestType private final int mRequestType;
    @Nullable private final String mStripeAccount;

    @NonNull
    static RequestOptions createForFingerprinting(@NonNull String guid) {
        return new RequestOptions(guid);
    }

    @NonNull
    static RequestOptions createForApi(@NonNull String apiKey) {
        return new RequestOptions(apiKey, null);
    }

    @NonNull
    static RequestOptions createForApi(
            @NonNull String apiKey,
            @Nullable String stripeAccount) {
        return new RequestOptions(apiKey, stripeAccount);
    }

    /**
     * Constructor for {@link RequestType#API}
     */
    private RequestOptions(
            @NonNull String apiKey,
            @Nullable String stripeAccount) {
        mRequestType = RequestType.API;
        mApiKey = new ApiKeyValidator().requireValid(apiKey);
        mStripeAccount = stripeAccount;
        mGuid = null;
    }

    /**
     * Constructor for {@link RequestType#FINGERPRINTING}
     */
    private RequestOptions(@NonNull String guid) {
        mRequestType = RequestType.FINGERPRINTING;
        mGuid = guid;
        mApiKey = null;
        mStripeAccount = null;
    }

    /**
     * @return the guid for this request
     */
    @Nullable
    String getGuid() {
        return mGuid;
    }

    /**
     * @return the publishable API key for this request
     */
    @Nullable
    String getApiKey() {
        return mApiKey;
    }

    @RequestType
    int getRequestType() {
        return mRequestType;
    }

    @Nullable
    String getStripeAccount() {
        return mStripeAccount;
    }
}
