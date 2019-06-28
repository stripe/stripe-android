package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public final class SetupIntentParams implements StripeIntentParams {

    static final String API_PARAM_CLIENT_SECRET = "client_secret";
    static final String API_PARAM_RETURN_URL = "return_url";
    static final String API_PARAM_PAYMENT_METHOD_ID = "payment_method";

    @Nullable private String mClientSecret;
    @Nullable private String mPaymentMethodId;
    @Nullable private String mReturnUrl;

    private SetupIntentParams() {

    }

    /**
     * Create the parameters necessary for confirming a PaymentIntent while attaching a
     * PaymentMethod that already exits.
     *
     * @param paymentMethodId the ID of the PaymentMethod that is being attached to the
     *         PaymentIntent being confirmed
     * @param clientSecret client secret from the PaymentIntent being confirmed
     * @param returnUrl the URL the customer should be redirected to after the authorization
     *         process
     * @return params that can be use to confirm a PaymentIntent
     */
    @NonNull
    public static SetupIntentParams createConfirmSetupIntenParamsWithPaymentMethodId(
            @Nullable String paymentMethodId,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return new SetupIntentParams()
                .setPaymentMethodId(paymentMethodId)
                .setClientSecret(clientSecret)
                .setReturnUrl(returnUrl);
    }

    /**
     * Create the parameters necessary for retrieving the details of SetupIntent
     *
     * @param clientSecret client secret from the SetupIntent that is being retrieved
     * @return params that can be used to retrieve a SetupIntent
     */
    @NonNull
    public static SetupIntentParams createRetrieveSetupIntentParams(
            @NonNull String clientSecret) {
        return new SetupIntentParams().setClientSecret(clientSecret);
    }

    /**
     * Sets the client secret that is used to authenticate actions on this SetupIntent
     *
     * @param clientSecret client secret associated with this SetupIntent
     * @return {@code this}, for chaining purposes
     */
    public SetupIntentParams setClientSecret(@NonNull String clientSecret) {
        mClientSecret = clientSecret;
        return this;
    }

    /**
     * @return client secret associated with the SetupIntent, used to identify the SetupIntent
     *         and authenticate actions.
     */
    @Nullable
    public String getClientSecret() {
        return mClientSecret;
    }

    /**
     * Sets a pre-existing PaymentMethod that will be attached to this SetupIntent
     *
     * @param paymentMethodId The ID of the PaymentMethod that is being attached to this
     *         SetupIntent.
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public SetupIntentParams setPaymentMethodId(@Nullable String paymentMethodId) {
        mPaymentMethodId = paymentMethodId;
        return this;
    }

    /**
     * @return the ID of the existing PaymentMethod that is being attached to this SetupIntent.
     */
    @Nullable
    public String getPaymentMethodId() {
        return mPaymentMethodId;
    }

    /**
     * @param returnUrl the URL the customer should be redirected to after the authorization
     *         process
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public SetupIntentParams setReturnUrl(@NonNull String returnUrl) {
        mReturnUrl = returnUrl;
        return this;
    }

    /**
     * @return the URL the customer should be redirected to after the authorization process
     */
    @Nullable
    public String getReturnUrl() {
        return mReturnUrl;
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
}
