package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.utils.ObjectUtils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfirmPaymentIntentParams implements StripeIntentParams {

    public static final String API_PARAM_SOURCE_DATA = "source_data";
    public static final String API_PARAM_PAYMENT_METHOD_DATA = "payment_method_data";

    static final String API_PARAM_SOURCE_ID = "source";

    static final String API_PARAM_SAVE_PAYMENT_METHOD = "save_payment_method";

    @Nullable private final PaymentMethodCreateParams mPaymentMethodCreateParams;
    @Nullable private final String mPaymentMethodId;
    @Nullable private final SourceParams mSourceParams;
    @Nullable private final String mSourceId;

    @Nullable private final Map<String, Object> mExtraParams;
    @NonNull private final String mClientSecret;
    @Nullable private final String mReturnUrl;

    private final boolean mSavePaymentMethod;

    private ConfirmPaymentIntentParams(@NonNull Builder builder) {
        mClientSecret = builder.mClientSecret;
        mReturnUrl = builder.mReturnUrl;

        mPaymentMethodId = builder.mPaymentMethodId;
        mPaymentMethodCreateParams = builder.mPaymentMethodCreateParams;
        mSourceId = builder.mSourceId;
        mSourceParams = builder.mSourceParams;

        mSavePaymentMethod = builder.mSavePaymentMethod;

        mExtraParams = builder.mExtraParams;
    }

    /**
     * Create the parameters necessary for confirming a PaymentIntent while attaching a
     * PaymentMethod that already exits.
     *
     * @param paymentMethodId the ID of the PaymentMethod that is being attached to the
     *                        PaymentIntent being confirmed
     * @param clientSecret client secret from the PaymentIntent being confirmed
     * @param returnUrl the URL the customer should be redirected to after the authorization
     *                  process
     * @param savePaymentMethod Set to {@code true} to save this PaymentIntent’s payment method to
     *                          the associated Customer, if the payment method is not already
     *                          attached. This parameter only applies to the payment method passed
     *                          in the same request or the current payment method attached to the
     *                          PaymentIntent and must be specified again if a new payment method is
     *                          added.
     * @return params that can be use to confirm a PaymentIntent
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodId(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod,
            @Nullable Map<String, Object> extraParams) {
        return new Builder(clientSecret)
                .setPaymentMethodId(paymentMethodId)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build();
    }

    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodId(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod) {
        return createWithPaymentMethodId(paymentMethodId, clientSecret,
                returnUrl, savePaymentMethod, null);
    }

    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodId(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return createWithPaymentMethodId(paymentMethodId, clientSecret,
                returnUrl, false);
    }

    /**
     * Create the parameters necessary for confirming a PaymentIntent while attaching
     * {@link PaymentMethodCreateParams} data
     *
     * @param paymentMethodCreateParams params for the PaymentMethod that will be attached to this
     *                                  PaymentIntent
     * @param clientSecret client secret from the PaymentIntent that is to be confirmed
     * @param returnUrl the URL the customer should be redirected to after the authorization
     *                  process
     * @param savePaymentMethod Set to {@code true} to save this PaymentIntent’s payment method to
     *                          the associated Customer, if the payment method is not already
     *                          attached. This parameter only applies to the payment method passed
     *                          in the same request or the current payment method attached to the
     *                          PaymentIntent and must be specified again if a new payment method is
     *                          added.
     * @return params that can be use to confirm a PaymentIntent
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodCreateParams(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod,
            @Nullable Map<String, Object> extraParams) {
        return new Builder(clientSecret)
                .setPaymentMethodCreateParams(paymentMethodCreateParams)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build();
    }

    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodCreateParams(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod) {
        return createWithPaymentMethodCreateParams(paymentMethodCreateParams,
                clientSecret, returnUrl, savePaymentMethod, null);
    }

    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodCreateParams(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return createWithPaymentMethodCreateParams(paymentMethodCreateParams,
                clientSecret, returnUrl, false);
    }

    /**
     * Create the parameters necessary for confirming a PaymentIntent while attaching a source that
     * already exits.
     *
     * @param sourceId the ID of the source that is being attached to the PaymentIntent being
     *                 confirmed
     * @param clientSecret client secret from the PaymentIntent being confirmed
     * @param returnUrl the URL the customer should be redirected to after the authorization
     *                  process
     * @param savePaymentMethod Set to {@code true} to save this PaymentIntent’s source to the
     *                          associated Customer, if the source is not already attached.
     *                          This parameter only applies to the source passed in the same request
     *                          or the current source attached to the PaymentIntent and must be
     *                          specified again if a new source is added.
     * @return params that can be use to confirm a PaymentIntent
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithSourceId(
            @NonNull String sourceId,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod,
            @Nullable Map<String, Object> extraParams) {
        return new Builder(clientSecret)
                .setSourceId(sourceId)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build();
    }

    @NonNull
    public static ConfirmPaymentIntentParams createWithSourceId(
            @NonNull String sourceId,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod) {
        return createWithSourceId(sourceId, clientSecret, returnUrl, savePaymentMethod, null);
    }

    @NonNull
    public static ConfirmPaymentIntentParams createWithSourceId(
            @NonNull String sourceId,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return createWithSourceId(sourceId, clientSecret, returnUrl, false);
    }

    /**
     * Create the parameters necessary for confirming a PaymentIntent while attaching Source data
     *
     * @param sourceParams params for the source that will be attached to this PaymentIntent
     * @param clientSecret client secret from the PaymentIntent that is to be confirmed
     * @param returnUrl the URL the customer should be redirected to after the authorization
     *                  process
     * @param savePaymentMethod Set to {@code true} to save this PaymentIntent’s source to the
     *                          associated Customer, if the source is not already attached.
     *                          This parameter only applies to the source passed in the same request
     *                          or the current source attached to the PaymentIntent and must be
     *                          specified again if a new source is added.
     * @return params that can be use to confirm a PaymentIntent
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithSourceParams(
            @NonNull SourceParams sourceParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod,
            @Nullable Map<String, Object> extraParams) {
        return new Builder(clientSecret)
                .setSourceParams(sourceParams)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build();
    }

    @NonNull
    public static ConfirmPaymentIntentParams createWithSourceParams(
            @NonNull SourceParams sourceParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod) {
        return createWithSourceParams(sourceParams, clientSecret, returnUrl, savePaymentMethod,
                null);
    }

    @NonNull
    public static ConfirmPaymentIntentParams createWithSourceParams(
            @NonNull SourceParams sourceParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return createWithSourceParams(sourceParams, clientSecret, returnUrl, false);
    }

    @Nullable
    public PaymentMethodCreateParams getPaymentMethodCreateParams() {
        return mPaymentMethodCreateParams;
    }

    @Nullable
    public String getPaymentMethodId() {
        return mPaymentMethodId;
    }

    @Nullable
    public SourceParams getSourceParams() {
        return mSourceParams;
    }

    @Nullable
    public String getSourceId() {
        return mSourceId;
    }

    @Nullable
    public Map<String, Object> getExtraParams() {
        return mExtraParams;
    }

    @NonNull
    public String getClientSecret() {
        return mClientSecret;
    }

    @Nullable
    public String getReturnUrl() {
        return mReturnUrl;
    }

    public boolean shouldSavePaymentMethod() {
        return mSavePaymentMethod;
    }

    /**
     * Create a string-keyed map representing this object that is
     * ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    @NonNull
    public Map<String, Object> toParamMap() {
        final AbstractMap<String, Object> networkReadyMap = new HashMap<>();

        if (mPaymentMethodCreateParams != null) {
            networkReadyMap.put(API_PARAM_PAYMENT_METHOD_DATA,
                    mPaymentMethodCreateParams.toParamMap());
        } else if (mPaymentMethodId != null) {
            networkReadyMap.put(API_PARAM_PAYMENT_METHOD_ID, mPaymentMethodId);
        } else if (mSourceParams != null) {
            networkReadyMap.put(API_PARAM_SOURCE_DATA, mSourceParams.toParamMap());
        } else if (mSourceId != null) {
            networkReadyMap.put(API_PARAM_SOURCE_ID, mSourceId);
        }

        if (mReturnUrl != null) {
            networkReadyMap.put(API_PARAM_RETURN_URL, mReturnUrl);
        }
        networkReadyMap.put(API_PARAM_CLIENT_SECRET, mClientSecret);
        if (mExtraParams != null) {
            networkReadyMap.putAll(mExtraParams);
        }

        if (mSavePaymentMethod) {
            networkReadyMap.put(API_PARAM_SAVE_PAYMENT_METHOD, true);
        }

        return networkReadyMap;
    }

    private static final class Builder implements ObjectBuilder<ConfirmPaymentIntentParams> {
        @NonNull private final String mClientSecret;

        @Nullable private PaymentMethodCreateParams mPaymentMethodCreateParams;
        @Nullable private String mPaymentMethodId;
        @Nullable private SourceParams mSourceParams;
        @Nullable private String mSourceId;

        @Nullable private Map<String, Object> mExtraParams;
        @Nullable private String mReturnUrl;

        private boolean mSavePaymentMethod;

        /**
         * Sets the client secret that is used to authenticate actions on this PaymentIntent
         * @param clientSecret client secret associated with this PaymentIntent
         * @return {@code this}, for chaining purposes
         */
        private Builder(@NonNull String clientSecret) {
            mClientSecret = Objects.requireNonNull(clientSecret);
        }

        /**
         * Sets the PaymentMethod data that will be included with this PaymentIntent
         *
         * @param paymentMethodCreateParams Params for the PaymentMethod that will be attached to
         *                                  this PaymentIntent. Only one of PaymentMethodParam,
         *                                  PaymentMethodId, SourceParam, SourceId should be used
         *                                  at a time.
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        private Builder setPaymentMethodCreateParams(
                @NonNull PaymentMethodCreateParams paymentMethodCreateParams) {
            this.mPaymentMethodCreateParams = paymentMethodCreateParams;
            return this;
        }

        /**
         * Sets a pre-existing PaymentMethod that will be attached to this PaymentIntent
         *
         * @param paymentMethodId The ID of the PaymentMethod that is being attached to this
         *                        PaymentIntent. Only one of PaymentMethodParam, PaymentMethodId,
         *                        SourceParam, SourceId should be used at a time.
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        private Builder setPaymentMethodId(@NonNull String paymentMethodId) {
            mPaymentMethodId = paymentMethodId;
            return this;
        }

        /**
         * Sets the source data that will be included with this PaymentIntent
         *
         * @param sourceParams params for the source that will be attached to this PaymentIntent.
         *                     Only one of SourceParam and SourceId should be used at at time.
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        private Builder setSourceParams(@NonNull SourceParams sourceParams) {
            mSourceParams = sourceParams;
            return this;
        }

        /**
         * Sets a pre-existing source that will be attached to this PaymentIntent
         * @param sourceId the ID of the source that is being attached to this PaymentIntent. Only
         *                     one of SourceParam and SourceId should be used at at time
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        private Builder setSourceId(@Nullable String sourceId) {
            mSourceId = sourceId;
            return this;
        }

        /**
         * @param returnUrl the URL the customer should be redirected to after the authorization
         *                  process
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        private Builder setReturnUrl(@NonNull String returnUrl) {
            mReturnUrl = returnUrl;
            return this;
        }

        /**
         * @param extraParams params that will be included in the request. Incorrect params may
         *                    result in errors when connecting to Stripe's API.
         * @return {@code this}, for chaining purposes
         */
        @NonNull
        private Builder setExtraParams(@Nullable Map<String, Object> extraParams) {
            mExtraParams = extraParams;
            return this;
        }

        @NonNull
        private Builder setSavePaymentMethod(boolean savePaymentMethod) {
            mSavePaymentMethod = savePaymentMethod;
            return this;
        }

        @NonNull
        @Override
        public ConfirmPaymentIntentParams build() {
            return new ConfirmPaymentIntentParams(this);
        }
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof ConfirmPaymentIntentParams &&
                typedEquals((ConfirmPaymentIntentParams) obj));
    }

    private boolean typedEquals(@NonNull ConfirmPaymentIntentParams params) {
        return ObjectUtils.equals(mReturnUrl, params.mReturnUrl)
                && ObjectUtils.equals(mPaymentMethodCreateParams, params.mPaymentMethodCreateParams)
                && ObjectUtils.equals(mSourceParams, params.mSourceParams)
                && ObjectUtils.equals(mSourceId, params.mSourceId)
                && ObjectUtils.equals(mExtraParams, params.mExtraParams)
                && ObjectUtils.equals(mSavePaymentMethod, params.mSavePaymentMethod)
                && ObjectUtils.equals(mPaymentMethodId, params.mPaymentMethodId);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mPaymentMethodCreateParams, mSourceParams, mSourceId,
                mExtraParams, mReturnUrl, mClientSecret, mPaymentMethodId, mSavePaymentMethod);
    }
}
