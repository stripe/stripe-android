package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.utils.ObjectUtils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public final class ConfirmPaymentIntentParams implements ConfirmStripeIntentParams {

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
    private final boolean mUseStripeSdk;

    private ConfirmPaymentIntentParams(@NonNull Builder builder) {
        mClientSecret = builder.mClientSecret;
        mReturnUrl = builder.mReturnUrl;

        mPaymentMethodId = builder.mPaymentMethodId;
        mPaymentMethodCreateParams = builder.mPaymentMethodCreateParams;
        mSourceId = builder.mSourceId;
        mSourceParams = builder.mSourceParams;

        mSavePaymentMethod = builder.mSavePaymentMethod;

        mExtraParams = builder.mExtraParams;
        mUseStripeSdk = builder.mShouldUseSdk;
    }

    /**
     * Create a {@link ConfirmPaymentIntentParams} without a payment method.
     */
    @NonNull
    public static ConfirmPaymentIntentParams create(
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            @Nullable Map<String, Object> extraParams) {
        return new Builder(clientSecret)
                .setReturnUrl(returnUrl)
                .setExtraParams(extraParams)
                .build();
    }

    /**
     * See {@link #create(String, String, Map)}
     */
    @NonNull
    public static ConfirmPaymentIntentParams create(
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return create(clientSecret, returnUrl, null);
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
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodId(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret,
            @Nullable String returnUrl,
            boolean savePaymentMethod,
            @Nullable Map<String, Object> extraParams) {
        return new Builder(clientSecret)
                .setPaymentMethodId(paymentMethodId)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build();
    }

    /**
     * See {@link #createWithPaymentMethodId(String, String, String, boolean, Map)}
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodId(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret,
            @Nullable String returnUrl,
            boolean savePaymentMethod) {
        return createWithPaymentMethodId(paymentMethodId, clientSecret,
                returnUrl, savePaymentMethod, null);
    }

    /**
     * See {@link #createWithPaymentMethodId(String, String, String, boolean)}
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodId(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret,
            @Nullable String returnUrl) {
        return createWithPaymentMethodId(paymentMethodId, clientSecret,
                returnUrl, false);
    }

    /**
     * See {@link #createWithPaymentMethodId(String, String, String)}
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodId(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret) {
        return createWithPaymentMethodId(paymentMethodId, clientSecret, null);
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
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodCreateParams(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret,
            @Nullable String returnUrl,
            boolean savePaymentMethod,
            @Nullable Map<String, Object> extraParams) {
        return new Builder(clientSecret)
                .setPaymentMethodCreateParams(paymentMethodCreateParams)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod)
                .setExtraParams(extraParams)
                .build();
    }

    /**
     * See {@link #createWithPaymentMethodCreateParams(PaymentMethodCreateParams, String, String,
     * boolean, Map)}
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodCreateParams(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret,
            @Nullable String returnUrl,
            boolean savePaymentMethod) {
        return createWithPaymentMethodCreateParams(paymentMethodCreateParams,
                clientSecret, returnUrl, savePaymentMethod, null);
    }

    /**
     * See {@link #createWithPaymentMethodCreateParams(PaymentMethodCreateParams, String, String,
     * boolean)}
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodCreateParams(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret,
            @Nullable String returnUrl) {
        return createWithPaymentMethodCreateParams(paymentMethodCreateParams,
                clientSecret, returnUrl, false);
    }

    /**
     * See {@link #createWithPaymentMethodCreateParams(PaymentMethodCreateParams, String, String)}
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithPaymentMethodCreateParams(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret) {
        return createWithPaymentMethodCreateParams(paymentMethodCreateParams, clientSecret, null);
    }

    /**
     * Create the parameters necessary for confirming a PaymentIntent with an
     * existing {@link Source}.
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

    /**
     * See {@link #createWithSourceId(String, String, String, boolean, Map)}
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithSourceId(
            @NonNull String sourceId,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod) {
        return createWithSourceId(sourceId, clientSecret, returnUrl, savePaymentMethod, null);
    }

    /**
     * See {@link #createWithSourceId(String, String, String, boolean)}
     */
    @NonNull
    public static ConfirmPaymentIntentParams createWithSourceId(
            @NonNull String sourceId,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return createWithSourceId(sourceId, clientSecret, returnUrl, false);
    }

    /**
     * Create the parameters necessary for confirming a PaymentIntent with {@link SourceParams}
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

    @Override
    public boolean shouldUseStripeSdk() {
        return mUseStripeSdk;
    }

    @NonNull
    @Override
    public ConfirmPaymentIntentParams withShouldUseStripeSdk(boolean shouldUseStripeSdk) {
        return toBuilder()
                .setShouldUseSdk(shouldUseStripeSdk)
                .build();
    }

    /**
     * Create a Map representing this object that is prepared for the Stripe API.
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

        if (mUseStripeSdk) {
            networkReadyMap.put(API_PARAM_USE_STRIPE_SDK, true);
        }

        return networkReadyMap;
    }

    @NonNull
    private Builder toBuilder() {
        return new Builder(mClientSecret)
                .setReturnUrl(mReturnUrl)
                .setPaymentMethodId(mPaymentMethodId)
                .setPaymentMethodCreateParams(mPaymentMethodCreateParams)
                .setSourceId(mSourceId)
                .setSourceParams(mSourceParams)
                .setSavePaymentMethod(mSavePaymentMethod)
                .setExtraParams(mExtraParams);
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
        private boolean mShouldUseSdk;

        /**
         * Sets the client secret that is used to authenticate actions on this PaymentIntent
         * @param clientSecret client secret associated with this PaymentIntent
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
         */
        @NonNull
        private Builder setSourceParams(@NonNull SourceParams sourceParams) {
            mSourceParams = sourceParams;
            return this;
        }

        /**
         * Sets a pre-existing source that will be attached to this PaymentIntent
         * @param sourceId the ID of an existing Source that is being attached to this
         *                 PaymentIntent. Only one of SourceParam and SourceId should be used at a
         *                 time.
         */
        @NonNull
        private Builder setSourceId(@Nullable String sourceId) {
            mSourceId = sourceId;
            return this;
        }

        /**
         * @param returnUrl the URL the customer should be redirected to after the authentication
         *                  process
         */
        @NonNull
        private Builder setReturnUrl(@Nullable String returnUrl) {
            mReturnUrl = returnUrl;
            return this;
        }

        /**
         * @param extraParams params that will be included in the request. Incorrect params may
         *                    result in errors when connecting to Stripe's API.
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
        private Builder setShouldUseSdk(boolean shouldUseSdk) {
            mShouldUseSdk = shouldUseSdk;
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
                && ObjectUtils.equals(mClientSecret, params.mClientSecret)
                && ObjectUtils.equals(mPaymentMethodId, params.mPaymentMethodId)
                && ObjectUtils.equals(mPaymentMethodCreateParams, params.mPaymentMethodCreateParams)
                && ObjectUtils.equals(mSourceId, params.mSourceId)
                && ObjectUtils.equals(mSourceParams, params.mSourceParams)
                && ObjectUtils.equals(mExtraParams, params.mExtraParams)
                && ObjectUtils.equals(mSavePaymentMethod, params.mSavePaymentMethod)
                && ObjectUtils.equals(mUseStripeSdk, params.mUseStripeSdk);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mReturnUrl, mClientSecret, mPaymentMethodId,
                mPaymentMethodCreateParams, mSourceId, mSourceParams, mExtraParams,
                mSavePaymentMethod, mUseStripeSdk);
    }
}
