package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.utils.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfirmSetupIntentParams implements ConfirmStripeIntentParams {

    @NonNull private final String mClientSecret;
    @Nullable private final String mPaymentMethodId;
    @Nullable private final PaymentMethodCreateParams mPaymentMethodCreateParams;
    @Nullable private final String mReturnUrl;
    private final boolean mUseStripeSdk;

    private ConfirmSetupIntentParams(@NonNull Builder builder) {
        this.mClientSecret = builder.mClientSecret;
        this.mReturnUrl = builder.mReturnUrl;
        this.mPaymentMethodId = builder.mPaymentMethodId;
        this.mPaymentMethodCreateParams = builder.mPaymentMethodCreateParams;
        this.mUseStripeSdk = builder.mShouldUseSdk;
    }

    /**
     * Create the parameters necessary for confirming a SetupIntent while attaching a
     * PaymentMethod that already exits.
     *
     * @param paymentMethodId the ID of the PaymentMethod that is being attached to the
     *                        SetupIntent being confirmed
     * @param clientSecret client secret from the SetupIntent being confirmed
     * @param returnUrl the URL the customer should be redirected to after the authorization process
     * @return params that can be use to confirm a SetupIntent
     */
    @NonNull
    public static ConfirmSetupIntentParams create(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret,
            @Nullable String returnUrl) {
        return new ConfirmSetupIntentParams.Builder(clientSecret)
                .setReturnUrl(returnUrl)
                .setPaymentMethodId(paymentMethodId)
                .build();
    }

    /**
     * See {@link #create(String, String, String)}
     */
    @NonNull
    public static ConfirmSetupIntentParams create(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret) {
        return new ConfirmSetupIntentParams.Builder(clientSecret)
                .setPaymentMethodId(paymentMethodId)
                .build();
    }

    /**
     * Create the parameters necessary for confirming a SetupIntent with a new PaymentMethod
     *
     * @param paymentMethodCreateParams the params to create a new PaymentMethod that will be
     *                                  attached to the SetupIntent being confirmed
     * @param clientSecret client secret from the SetupIntent being confirmed
     * @param returnUrl the URL the customer should be redirected to after the authorization process
     * @return params that can be use to confirm a SetupIntent
     */
    @NonNull
    public static ConfirmSetupIntentParams create(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret,
            @Nullable String returnUrl) {
        return new ConfirmSetupIntentParams.Builder(clientSecret)
                .setPaymentMethodCreateParams(paymentMethodCreateParams)
                .setReturnUrl(returnUrl)
                .build();
    }

    /**
     * See {@link #create(PaymentMethodCreateParams, String, String)}
     */
    @NonNull
    public static ConfirmSetupIntentParams create(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret) {
        return create(paymentMethodCreateParams, clientSecret, null);
    }

    @NonNull
    public String getClientSecret() {
        return mClientSecret;
    }

    @Override
    public boolean shouldUseStripeSdk() {
        return mUseStripeSdk;
    }

    @NonNull
    @Override
    public ConfirmSetupIntentParams withShouldUseStripeSdk(boolean shouldUseStripeSdk) {
        return toBuilder()
                .setShouldUseSdk(shouldUseStripeSdk)
                .build();
    }

    /**
     * Create a string-keyed map representing this object that is
     * ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    @NonNull
    @Override
    public Map<String, Object> toParamMap() {
        final Map<String, Object> networkReadyMap = new HashMap<>();

        if (mPaymentMethodCreateParams != null) {
            networkReadyMap.put(API_PARAM_PAYMENT_METHOD_DATA,
                    mPaymentMethodCreateParams.toParamMap());
        } else if (mPaymentMethodId != null) {
            networkReadyMap.put(API_PARAM_PAYMENT_METHOD_ID, mPaymentMethodId);
        }

        networkReadyMap.put(API_PARAM_CLIENT_SECRET, mClientSecret);
        if (mReturnUrl != null) {
            networkReadyMap.put(API_PARAM_RETURN_URL, mReturnUrl);
        }

        if (mUseStripeSdk) {
            networkReadyMap.put(API_PARAM_USE_STRIPE_SDK, true);
        }

        return networkReadyMap;
    }

    @Nullable
    public PaymentMethodCreateParams getPaymentMethodCreateParams() {
        return mPaymentMethodCreateParams;
    }

    @NonNull
    @VisibleForTesting
    ConfirmSetupIntentParams.Builder toBuilder() {
        return new ConfirmSetupIntentParams.Builder(mClientSecret)
                .setReturnUrl(mReturnUrl)
                .setPaymentMethodId(mPaymentMethodId)
                .setPaymentMethodCreateParams(mPaymentMethodCreateParams)
                .setShouldUseSdk(mUseStripeSdk);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof ConfirmSetupIntentParams &&
                typedEquals((ConfirmSetupIntentParams) obj));
    }

    private boolean typedEquals(@NonNull ConfirmSetupIntentParams params) {
        return ObjectUtils.equals(mReturnUrl, params.mReturnUrl)
                && ObjectUtils.equals(mClientSecret, params.mClientSecret)
                && ObjectUtils.equals(mPaymentMethodId, params.mPaymentMethodId)
                && ObjectUtils.equals(mPaymentMethodCreateParams, params.mPaymentMethodCreateParams)
                && ObjectUtils.equals(mUseStripeSdk, params.mUseStripeSdk);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mReturnUrl, mClientSecret, mPaymentMethodId, mUseStripeSdk);
    }


    @VisibleForTesting
    static final class Builder implements ObjectBuilder<ConfirmSetupIntentParams> {
        @NonNull private final String mClientSecret;
        @Nullable private String mPaymentMethodId;
        @Nullable private PaymentMethodCreateParams mPaymentMethodCreateParams;
        @Nullable private String mReturnUrl;
        private boolean mShouldUseSdk;

        private Builder(@NonNull String clientSecret) {
            mClientSecret = Objects.requireNonNull(clientSecret);
        }

        @NonNull
        private ConfirmSetupIntentParams.Builder setPaymentMethodId(
                @NonNull String paymentMethodId) {
            mPaymentMethodId = paymentMethodId;
            return this;
        }

        @NonNull
        private ConfirmSetupIntentParams.Builder setPaymentMethodCreateParams(
                @NonNull PaymentMethodCreateParams paymentMethodCreateParams) {
            mPaymentMethodCreateParams = paymentMethodCreateParams;
            return this;
        }

        @NonNull
        private ConfirmSetupIntentParams.Builder setReturnUrl(@Nullable String returnUrl) {
            mReturnUrl = returnUrl;
            return this;
        }

        @NonNull
        private ConfirmSetupIntentParams.Builder setShouldUseSdk(boolean shouldUseSdk) {
            mShouldUseSdk = shouldUseSdk;
            return this;
        }

        @NonNull
        @Override
        public ConfirmSetupIntentParams build() {
            return new ConfirmSetupIntentParams(this);
        }
    }

}
