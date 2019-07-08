package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public final class SetupIntentParams implements StripeIntentParams {

    @NonNull private final String mClientSecret;
    @Nullable private final String mReturnUrl;
    @Nullable private final String mPaymentMethodId;

    private SetupIntentParams(@NonNull String clientSecret, @Nullable String returnUrl,
                              @Nullable String paymentMethodId) {
        this.mClientSecret = clientSecret;
        this.mReturnUrl = returnUrl;
        this.mPaymentMethodId = paymentMethodId;
    }

    private SetupIntentParams(@NonNull String clientSecret) {
        this.mClientSecret = clientSecret;
        this.mReturnUrl = null;
        this.mPaymentMethodId = null;
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
    public static SetupIntentParams createConfirmParams(
            @NonNull String paymentMethodId,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return new SetupIntentParams(clientSecret, returnUrl, paymentMethodId);
    }

    /**
     * Create the parameters necessary for retrieving the details of SetupIntent
     *
     * @param clientSecret client secret from the SetupIntent that is being retrieved
     * @return params that can be used to retrieve a SetupIntent
     */
    @NonNull
    public static SetupIntentParams createRetrieveParams(@NonNull String clientSecret) {
        return new SetupIntentParams(clientSecret);
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
        final AbstractMap<String, Object> networkReadyMap = new HashMap<>();

        if (mPaymentMethodId != null) {
            networkReadyMap.put(API_PARAM_PAYMENT_METHOD_ID, mPaymentMethodId);
        }

        if (mReturnUrl != null) {
            networkReadyMap.put(API_PARAM_RETURN_URL, mReturnUrl);
        }

        networkReadyMap.put(API_PARAM_CLIENT_SECRET, mClientSecret);

        return networkReadyMap;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof SetupIntentParams &&
                typedEquals((SetupIntentParams) obj));
    }

    private boolean typedEquals(@NonNull SetupIntentParams setupIntentParams) {
        return ObjectUtils.equals(mReturnUrl, setupIntentParams.mReturnUrl)
                && ObjectUtils.equals(mClientSecret, setupIntentParams.mClientSecret)
                && ObjectUtils.equals(mPaymentMethodId, setupIntentParams.mPaymentMethodId);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mReturnUrl, mClientSecret, mPaymentMethodId);
    }
}
