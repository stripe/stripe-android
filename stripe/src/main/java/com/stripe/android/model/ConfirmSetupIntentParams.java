package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

public final class ConfirmSetupIntentParams implements StripeIntentParams {

    @NonNull private final String mClientSecret;
    @Nullable private final String mReturnUrl;
    @NonNull private final String mPaymentMethodId;

    private ConfirmSetupIntentParams(@NonNull String clientSecret, @Nullable String returnUrl,
                                     @NonNull String paymentMethodId) {
        this.mClientSecret = clientSecret;
        this.mReturnUrl = returnUrl;
        this.mPaymentMethodId = paymentMethodId;
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
        return new ConfirmSetupIntentParams(clientSecret, returnUrl, paymentMethodId);
    }

    @NonNull
    public String getClientSecret() {
        return mClientSecret;
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

        return networkReadyMap;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof ConfirmSetupIntentParams &&
                typedEquals((ConfirmSetupIntentParams) obj));
    }

    private boolean typedEquals(@NonNull ConfirmSetupIntentParams confirmSetupIntentParams) {
        return ObjectUtils.equals(mReturnUrl, confirmSetupIntentParams.mReturnUrl)
                && ObjectUtils.equals(mClientSecret, confirmSetupIntentParams.mClientSecret)
                && ObjectUtils.equals(mPaymentMethodId, confirmSetupIntentParams.mPaymentMethodId);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mReturnUrl, mClientSecret, mPaymentMethodId);
    }
}
