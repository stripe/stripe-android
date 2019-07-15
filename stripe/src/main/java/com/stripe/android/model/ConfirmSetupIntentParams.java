package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.utils.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfirmSetupIntentParams implements ConfirmStripeIntentParams {

    @NonNull private final String mClientSecret;
    @Nullable private final String mReturnUrl;
    @NonNull private final String mPaymentMethodId;
    private final boolean mUseStripeSdk;

    private ConfirmSetupIntentParams(@NonNull Builder builder) {
        this.mClientSecret = builder.mClientSecret;
        this.mReturnUrl = builder.mReturnUrl;
        this.mPaymentMethodId = Objects.requireNonNull(builder.mPaymentMethodId);
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
            @NonNull String returnUrl) {
        return new ConfirmSetupIntentParams.Builder(clientSecret)
                .setReturnUrl(returnUrl)
                .setPaymentMethodId(paymentMethodId)
                .build();
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
    public Map<String, Object> toParamMap() {
        final Map<String, Object> networkReadyMap = new HashMap<>();
        networkReadyMap.put(API_PARAM_PAYMENT_METHOD_ID, mPaymentMethodId);
        networkReadyMap.put(API_PARAM_CLIENT_SECRET, mClientSecret);
        if (mReturnUrl != null) {
            networkReadyMap.put(API_PARAM_RETURN_URL, mReturnUrl);
        }

        if (mUseStripeSdk) {
            networkReadyMap.put(API_PARAM_USE_STRIPE_SDK, true);
        }

        return networkReadyMap;
    }

    @NonNull
    private ConfirmSetupIntentParams.Builder toBuilder() {
        return new ConfirmSetupIntentParams.Builder(mClientSecret)
                .setReturnUrl(mReturnUrl)
                .setPaymentMethodId(mPaymentMethodId)
                .setShouldUseSdk(mUseStripeSdk);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof ConfirmSetupIntentParams &&
                typedEquals((ConfirmSetupIntentParams) obj));
    }

    private boolean typedEquals(@NonNull ConfirmSetupIntentParams confirmSetupIntentParams) {
        return ObjectUtils.equals(mReturnUrl, confirmSetupIntentParams.mReturnUrl)
                && ObjectUtils.equals(mClientSecret, confirmSetupIntentParams.mClientSecret)
                && ObjectUtils.equals(mPaymentMethodId, confirmSetupIntentParams.mPaymentMethodId)
                && ObjectUtils.equals(mUseStripeSdk, confirmSetupIntentParams.mUseStripeSdk);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mReturnUrl, mClientSecret, mPaymentMethodId, mUseStripeSdk);
    }


    private static final class Builder implements ObjectBuilder<ConfirmSetupIntentParams> {
        @NonNull private final String mClientSecret;
        @Nullable private String mPaymentMethodId;
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
        private ConfirmSetupIntentParams.Builder setReturnUrl(@NonNull String returnUrl) {
            mReturnUrl = returnUrl;
            return this;
        }

        @NonNull
        public ConfirmSetupIntentParams.Builder setShouldUseSdk(boolean shouldUseSdk) {
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
