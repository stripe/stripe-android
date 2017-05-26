package com.stripe.android.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.util.StripeTextUtils;

/**
 * Data class representing options for a Stripe API request.
 */
public class RequestOptions {

    @NonNull private final String mApiVersion;
    @Nullable private final String mGuid;
    @Nullable private final String mIdempotencyKey;
    @Nullable private final String mPublishableApiKey;

    private RequestOptions(
            @NonNull String apiVersion,
            @Nullable String guid,
            @Nullable String idempotencyKey,
            @Nullable String publishableApiKey) {
        mApiVersion = apiVersion;
        mGuid = guid;
        mIdempotencyKey = idempotencyKey;
        mPublishableApiKey = publishableApiKey;
    }

    /**
     * @return the API version for this request
     */
    @Nullable
    public String getApiVersion() {
        return mApiVersion;
    }

    /**
     * @return the guid for this request
     */
    @Nullable
    public String getGuid() {
        return mGuid;
    }

    /**
     * @return the idempotency key for this request
     */
    @Nullable
    public String getIdempotencyKey() {
        return mIdempotencyKey;
    }

    /**
     * @return the publishable API key for this request
     */
    @Nullable
    public String getPublishableApiKey() {
        return mPublishableApiKey;
    }

    /**
     * Static accessor for the {@link RequestOptionsBuilder} class.
     *
     * @param publishableApiKey your publishable API key
     * @return a {@link RequestOptionsBuilder} instance
     */
    public static RequestOptions.RequestOptionsBuilder builder(@Nullable String publishableApiKey) {
        return new RequestOptions.RequestOptionsBuilder(publishableApiKey);
    }

    /**
     * Builder class for a set of {@link RequestOptions}.
     */
    public static final class RequestOptionsBuilder {

        private String publishableApiKey;
        private String guid;
        private String idempotencyKey;
        private String apiVersion;

        /**
         * Builder constructor requiring an API key.
         *
         * @param publishableApiKey your publishable API key
         */
        public RequestOptionsBuilder(@Nullable String publishableApiKey) {
            this.publishableApiKey = publishableApiKey;
        }

        /**
         * A way to set your publishable key outside of the constructor.
         *
         * @param publishableApiKey your publishable API key
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        public RequestOptionsBuilder setPublishableApiKey(@NonNull String publishableApiKey) {
            this.publishableApiKey = publishableApiKey;
            return this;
        }

        /**
         * Setter for the optional idempotency value of the {@link RequestOptions}. This can
         * be any value you want.
         *
         * @param idempotencyKey the idempotency key
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        public RequestOptionsBuilder setIdempotencyKey(@Nullable String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        /**
         * Setter for the optional guid value of the {@link RequestOptions}.
         *
         * @param guid the guid
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        public RequestOptionsBuilder setGuid(@Nullable String guid) {
            this.guid = guid;
            return this;
        }

        /**
         * Setter for the API version for this set of {@link RequestOptions}. If not set,
         * your account default API version is used.
         *
         * @param apiVersion the API version to use
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        public RequestOptionsBuilder setApiVersion(@Nullable String apiVersion) {
            this.apiVersion = StripeTextUtils.isBlank(apiVersion)
                    ? null
                    : apiVersion;
            return this;
        }

        /**
         * Construct the {@link RequestOptions} object.
         *
         * @return the new {@link RequestOptions} object
         */
        public RequestOptions build() {
            return new RequestOptions(this.apiVersion, this.guid, this.idempotencyKey, this.publishableApiKey);
        }
    }
}
