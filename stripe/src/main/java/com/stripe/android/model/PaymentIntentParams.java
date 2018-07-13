package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PaymentIntentParams {

    static final String API_PARAM_SOURCE_DATA = "source_data";
    static final String API_PARAM_SOURCE_ID = "source";
    static final String API_PARAM_RETURN_URL = "return_url";
    static final String API_PARAM_CLIENT_SECRET = "client_secret";

    private Map<String, Object> mExtraParams;
    private SourceParams mSourceParams;
    private String mSourceId;
    private String mClientSecret;
    private String mReturnUrl;

    private PaymentIntentParams() {
    }

    /**
     * Create a custom {@link PaymentIntentParams}. Incorrect attributes may result in errors
     * when connecting to Stripe's API.
     *
     * @return an empty Params object. Call the setter methods on this class to specific values on
     * the params
     */
    public static PaymentIntentParams createCustomParams() {
        return new PaymentIntentParams();
    }

    /**
     * Create the parameters necessary for confirming a PaymentIntent while attaching Source data
     *
     * @param sourceParams params for the source that will be attached to this payment intent
     * @param clientSecret client secret from the PaymentIntent that is to be confirmed
     * @param returnUrl the URL the customer should be redirected to after the authorization
     *                  process
     * @return params that can be use to confirm a PaymentIntent
     */
    @NonNull
    public static PaymentIntentParams createConfirmPaymentIntentWithSourceDataParams(
            @Nullable SourceParams sourceParams,
            @NonNull String clientSecret,
            @Nullable String returnUrl) {
        return new PaymentIntentParams()
                .setSourceParams(sourceParams)
                .setClientSecret(clientSecret)
                .setReturnUrl(returnUrl);
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
     * @return params that can be use to confirm a PaymentIntent
     */
    @NonNull
    public static PaymentIntentParams createConfirmPaymentIntentWithSourceIdParams(
            @Nullable String sourceId,
            @NonNull String clientSecret,
            @Nullable String returnUrl) {
        return new PaymentIntentParams()
                .setSourceId(sourceId)
                .setClientSecret(clientSecret)
                .setReturnUrl(returnUrl);
    }

    /**
     * Create the parameters necessary for retrieving the details of PaymentIntent
     *
     * @param clientSecret client secret from the PaymentIntent that is being retrieved
     * @return params that can be used to retrieve a PaymentIntent
     */
    @NonNull
    public static PaymentIntentParams createRetrievePaymentIntentParams(
            @NonNull String clientSecret) {
        return new PaymentIntentParams().setClientSecret(clientSecret);
    }

    /**
     * Sets the source data that will be included with this PaymentIntent
     *
     * @param sourceParams params for the source that will be attached to this payment intent. Only
     *                     one of SourceParam and SourceId should be used at at time
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public PaymentIntentParams setSourceParams(@NonNull SourceParams sourceParams) {
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
    public PaymentIntentParams setSourceId(@NonNull String sourceId) {
        mSourceId = sourceId;
        return this;
    }

    /**
     * Sets the client secret that is used to authenticate actions on this PaymentIntent
     * @param clientSecret client secret associated with this PaymentIntent
     * @return {@code this}, for chaining purposes
     */
    public PaymentIntentParams setClientSecret(@NonNull String clientSecret) {
        mClientSecret = clientSecret;
        return this;
    }

    /**
     * @param returnUrl the URL the customer should be redirected to after the authorization
     *                  process
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public PaymentIntentParams setReturnUrl(@NonNull String returnUrl) {
        mReturnUrl = returnUrl;
        return this;
    }

    /**
     * @param extraParams params that will be included in the request. Incorrect params may result
     *                    in errors when connecting to Stripe's API.
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public PaymentIntentParams setExtraParams(@NonNull Map<String, Object> extraParams) {
        mExtraParams = extraParams;
        return this;
    }

    /**
     * Create a string-keyed map representing this object that is
     * ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    @NonNull
    public Map<String, Object> toParamMap() {
        Map<String, Object> networkReadyMap = new HashMap<>();
        if (mSourceParams != null) {
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
        return networkReadyMap;
    }

    /**
     * @return client secret associated with the PaymentIntent, used to identify the PaymentIntent
     * and authenticate actions.
     */
    @Nullable
    public String getClientSecret() {
        return mClientSecret;
    }

    /**
     * @return extraparams that will be included in the request
     */
    @Nullable
    public Map<String, Object> getExtraParams() {
        return mExtraParams;
    }

    /**
     * @return params for the source that will be attached to this payment intent
     */
    @Nullable
    public SourceParams getSourceParams() {
        return mSourceParams;
    }

    /**
     * @return the ID of the existing source that is being attached to this PaymentIntent.
     */
    @Nullable
    public String getSourceId() {
        return mSourceId;
    }

    /**
     * @return  the URL the customer should be redirected to after the authorization process
     */
    @Nullable
    public String getReturnUrl() {
        return mReturnUrl;
    }

}
