package com.stripe.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data class representing options for a Stripe API request.
 */
public class RequestOptions {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE_QUERY, TYPE_JSON})
    public @interface RequestType {}
    public static final String TYPE_QUERY = "source";
    public static final String TYPE_JSON = "json_data";

    @Nullable private final String mGuid;
    @Nullable private final String mIdempotencyKey;
    @Nullable private final String mPublishableApiKey;
    @NonNull @RequestType private final String mRequestType;
    @Nullable private final String mStripeAccount;

    private RequestOptions(
            @Nullable String guid,
            @Nullable String idempotencyKey,
            @Nullable String publishableApiKey,
            @NonNull @RequestType String requestType,
            @Nullable String stripeAccount) {
        mGuid = guid;
        mIdempotencyKey = idempotencyKey;
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
     * @return the idempotency key for this request
     */
    @Nullable
    String getIdempotencyKey() {
        return mIdempotencyKey;
    }

    /**
     * @return the publishable API key for this request
     */
    @Nullable
    String getPublishableApiKey() {
        return mPublishableApiKey;
    }

    @NonNull
    @RequestType
    String getRequestType() {
        return mRequestType;
    }

    @Nullable
    String getStripeAccount() {
        return mStripeAccount;
    }

    /**
     * Static accessor for the {@link RequestOptionsBuilder} class. Creates
     * a builder for a {@link #TYPE_QUERY} options item
     *
     * @param publishableApiKey your publishable API key
     * @return a {@link RequestOptionsBuilder} instance
     */
    public static RequestOptions.RequestOptionsBuilder builder(@Nullable String publishableApiKey) {
        return builder(publishableApiKey, TYPE_QUERY);
    }

    @NonNull
    public static RequestOptions.RequestOptionsBuilder builder(
            @Nullable String publishableApiKey,
            @Nullable String stripeAccount,
            @NonNull @RequestType String requestType) {
        return new RequestOptionsBuilder(publishableApiKey, requestType)
                .setStripeAccount(stripeAccount);
    }

    /**
     * Static accessor for the {@link RequestOptionsBuilder} class with type.
     *
     * @param publishableApiKey your publishable API key
     * @param requestType your {@link RequestType}
     * @return a {@link RequestOptionsBuilder} instance
     */
    public static RequestOptions.RequestOptionsBuilder builder(
            @Nullable String publishableApiKey,
            @NonNull @RequestType String requestType) {
        return new RequestOptions.RequestOptionsBuilder(
                publishableApiKey,
                requestType);
    }

    /**
     * Builder class for a set of {@link RequestOptions}.
     */
    public static final class RequestOptionsBuilder {

        private String guid;
        private String idempotencyKey;
        private String publishableApiKey;
        private @RequestType String requestType;
        private String stripeAccount;

        /**
         * Builder constructor requiring an API key.
         *
         * @param publishableApiKey your publishable API key
         */
        RequestOptionsBuilder(
                @Nullable String publishableApiKey,
                @NonNull @RequestType String requestType) {
            this.publishableApiKey = publishableApiKey;
            this.requestType = requestType;
        }

        /**
         * A way to set your publishable key outside of the constructor.
         *
         * @param publishableApiKey your publishable API key
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        RequestOptionsBuilder setPublishableApiKey(@NonNull String publishableApiKey) {
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
        RequestOptionsBuilder setIdempotencyKey(@Nullable String idempotencyKey) {
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
        RequestOptionsBuilder setGuid(@Nullable String guid) {
            this.guid = guid;
            return this;
        }

        @NonNull
        RequestOptionsBuilder setStripeAccount(@Nullable String stripeAccount) {
            this.stripeAccount = stripeAccount;
            return this;
        }

        /**
         * Construct the {@link RequestOptions} object.
         *
         * @return the new {@link RequestOptions} object
         */
        public RequestOptions build() {
            return new RequestOptions(
                    this.guid,
                    this.idempotencyKey,
                    this.publishableApiKey,
                    this.requestType,
                    this.stripeAccount);
        }
    }
}
