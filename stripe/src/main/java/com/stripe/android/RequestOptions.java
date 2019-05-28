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
    @Nullable private final String mPublishableApiKey;
    @RequestType private final int mRequestType;
    @Nullable private final String mStripeAccount;

    @NonNull
    static RequestOptions createForFingerprinting(@NonNull String guid) {
        return new RequestOptions(RequestType.FINGERPRINTING, null, null, guid);
    }

    @NonNull
    static RequestOptions createForApi(@NonNull String publishableApiKey) {
        return new RequestOptions(RequestType.API, publishableApiKey, null, null);
    }

    @NonNull
    static RequestOptions createForApi(
            @NonNull String publishableApiKey,
            @Nullable String stripeAccount) {
        return new RequestOptions(RequestType.API, publishableApiKey, stripeAccount, null);
    }

    private RequestOptions(
            @RequestType int requestType,
            @Nullable String publishableApiKey,
            @Nullable String stripeAccount,
            @Nullable String guid) {
        mGuid = guid;
        mPublishableApiKey = publishableApiKey;
        mRequestType = requestType;
        mStripeAccount = stripeAccount;
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
    String getPublishableApiKey() {
        return mPublishableApiKey;
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
