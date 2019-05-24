package com.stripe.android;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data class representing options for a Stripe API request.
 */
class RequestOptions {

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

    private RequestOptions(
            @Nullable String guid,
            @Nullable String publishableApiKey,
            @RequestType int requestType,
            @Nullable String stripeAccount) {
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

    /**
     * Static accessor for the {@link Builder} class. Creates
     * a builder for a {@link RequestType#API} options item
     *
     * @param publishableApiKey your publishable API key
     * @return a {@link Builder} instance
     */
    @NonNull
    public static Builder builder(@Nullable String publishableApiKey) {
        return builder(publishableApiKey, RequestType.API);
    }

    @NonNull
    public static Builder builder(
            @Nullable String publishableApiKey,
            @Nullable String stripeAccount,
            @RequestType int requestType) {
        return new Builder(publishableApiKey, requestType)
                .setStripeAccount(stripeAccount);
    }

    /**
     * Static accessor for the {@link Builder} class with type.
     *
     * @param publishableApiKey your publishable API key
     * @param requestType your {@link RequestType}
     * @return a {@link Builder} instance
     */
    @NonNull
    public static Builder builder(
            @Nullable String publishableApiKey,
            @RequestType int requestType) {
        return new Builder(
                publishableApiKey,
                requestType);
    }

    /**
     * Builder class for a set of {@link RequestOptions}.
     */
    static final class Builder {
        private String guid;
        private String publishableApiKey;
        @RequestType private int requestType;
        private String stripeAccount;

        /**
         * Builder constructor requiring an API key.
         *
         * @param publishableApiKey your publishable API key
         */
        Builder(@Nullable String publishableApiKey,
                @RequestType int requestType) {
            this.publishableApiKey = publishableApiKey;
            this.requestType = requestType;
        }

        /**
         * Setter for the optional guid value of the {@link RequestOptions}.
         *
         * @param guid the guid
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        Builder setGuid(@Nullable String guid) {
            this.guid = guid;
            return this;
        }

        @NonNull
        Builder setStripeAccount(@Nullable String stripeAccount) {
            this.stripeAccount = stripeAccount;
            return this;
        }

        /**
         * Construct the {@link RequestOptions} object.
         *
         * @return the new {@link RequestOptions} object
         */
        @NonNull
        public RequestOptions build() {
            return new RequestOptions(
                    this.guid,
                    this.publishableApiKey,
                    this.requestType,
                    this.stripeAccount);
        }
    }
}
