package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class PaymentIntentParams {

    static final String API_PARAM_PAYMENT_METHOD_DATA = "payment_method_data";
    static final String API_PARAM_PAYMENT_METHOD_ID = "payment_method";

    static final String API_PARAM_SOURCE_DATA = "source_data";
    static final String API_PARAM_SOURCE_ID = "source";

    static final String API_PARAM_RETURN_URL = "return_url";
    static final String API_PARAM_CLIENT_SECRET = "client_secret";

    static final String API_PARAM_SAVE_PAYMENT_METHOD = "save_payment_method";

    @Nullable private PaymentMethodCreateParams mPaymentMethodCreateParams;
    @Nullable private String mPaymentMethodId;
    @Nullable private SourceParams mSourceParams;
    @Nullable private String mSourceId;

    @Nullable private Map<String, Object> mExtraParams;
    @Nullable private String mClientSecret;
    @Nullable private String mReturnUrl;

    private boolean mSavePaymentMethod;

    private PaymentIntentParams() {
    }

    /**
     * Create a custom {@link PaymentIntentParams}. Incorrect attributes may result in errors
     * when connecting to Stripe's API.
     *
     * @return an empty Params object. Call the setter methods on this class to specific values on
     * the params
     */
    @NonNull
    public static PaymentIntentParams createCustomParams() {
        return new PaymentIntentParams();
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
    public static PaymentIntentParams createConfirmPaymentIntentWithPaymentMethodId(
            @Nullable String paymentMethodId,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod) {
        return new PaymentIntentParams()
                .setPaymentMethodId(paymentMethodId)
                .setClientSecret(clientSecret)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod);
    }

    @NonNull
    public static PaymentIntentParams createConfirmPaymentIntentWithPaymentMethodId(
            @Nullable String paymentMethodId,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return createConfirmPaymentIntentWithPaymentMethodId(paymentMethodId, clientSecret,
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
    public static PaymentIntentParams createConfirmPaymentIntentWithPaymentMethodCreateParams(
            @Nullable PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod) {
        return new PaymentIntentParams()
                .setPaymentMethodCreateParams(paymentMethodCreateParams)
                .setClientSecret(clientSecret)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod);
    }

    @NonNull
    public static PaymentIntentParams createConfirmPaymentIntentWithPaymentMethodCreateParams(
            @Nullable PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return createConfirmPaymentIntentWithPaymentMethodCreateParams(paymentMethodCreateParams,
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
    public static PaymentIntentParams createConfirmPaymentIntentWithSourceIdParams(
            @Nullable String sourceId,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod) {
        return new PaymentIntentParams()
                .setSourceId(sourceId)
                .setClientSecret(clientSecret)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod);
    }

    @NonNull
    public static PaymentIntentParams createConfirmPaymentIntentWithSourceIdParams(
            @Nullable String sourceId,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return createConfirmPaymentIntentWithSourceIdParams(sourceId, clientSecret, returnUrl,
                false);
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
    public static PaymentIntentParams createConfirmPaymentIntentWithSourceDataParams(
            @Nullable SourceParams sourceParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl,
            boolean savePaymentMethod) {
        return new PaymentIntentParams()
                .setSourceParams(sourceParams)
                .setClientSecret(clientSecret)
                .setReturnUrl(returnUrl)
                .setSavePaymentMethod(savePaymentMethod);
    }

    @NonNull
    public static PaymentIntentParams createConfirmPaymentIntentWithSourceDataParams(
            @Nullable SourceParams sourceParams,
            @NonNull String clientSecret,
            @NonNull String returnUrl) {
        return createConfirmPaymentIntentWithSourceDataParams(sourceParams, clientSecret, returnUrl,
                false);
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
        return new PaymentIntentParams()
                .setClientSecret(clientSecret);
    }

    /**
     * Sets the PaymentMethod data that will be included with this PaymentIntent
     *
     * @param paymentMethodCreateParams Params for the PaymentMethod that will be attached to this
     *                                  PaymentIntent. Only one of PaymentMethodParam,
     *                                  PaymentMethodId, SourceParam, SourceId should be used at a
     *                                  time.
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public PaymentIntentParams setPaymentMethodCreateParams(
            @Nullable PaymentMethodCreateParams paymentMethodCreateParams) {
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
    public PaymentIntentParams setPaymentMethodId(@Nullable String paymentMethodId) {
        mPaymentMethodId = paymentMethodId;
        return this;
    }

    /**
     * Sets the source data that will be included with this PaymentIntent
     *
     * @param sourceParams params for the source that will be attached to this PaymentIntent. Only
     *                     one of SourceParam and SourceId should be used at at time
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public PaymentIntentParams setSourceParams(@Nullable SourceParams sourceParams) {
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
    public PaymentIntentParams setSourceId(@Nullable String sourceId) {
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
    public PaymentIntentParams setExtraParams(@Nullable Map<String, Object> extraParams) {
        mExtraParams = extraParams;
        return this;
    }

    @NonNull
    public PaymentIntentParams setSavePaymentMethod(boolean savePaymentMethod) {
        mSavePaymentMethod = savePaymentMethod;
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
     * @return params for the source that will be attached to this PaymentIntent
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
     * @return params for the PaymentMethod that will be attached to this PaymentIntent
     */
    @Nullable
    public PaymentMethodCreateParams getPaymentMethodCreateParams() {
        return mPaymentMethodCreateParams;
    }

    /**
     * @return the ID of the existing PaymentMethod that is being attached to this PaymentIntent.
     */
    @Nullable
    public String getPaymentMethodId() {
        return mPaymentMethodId;
    }

    /**
     * @return  the URL the customer should be redirected to after the authorization process
     */
    @Nullable
    public String getReturnUrl() {
        return mReturnUrl;
    }

    public boolean shouldSavePaymentMethod() {
        return mSavePaymentMethod;
    }
}
