package com.stripe.android.model;

import android.support.annotation.NonNull;

import java.util.Map;

public interface ConfirmStripeIntentParams {

    String API_PARAM_CLIENT_SECRET = "client_secret";
    String API_PARAM_RETURN_URL = "return_url";
    String API_PARAM_PAYMENT_METHOD_ID = "payment_method";
    String API_PARAM_PAYMENT_METHOD_DATA = "payment_method_data";
    String API_PARAM_USE_STRIPE_SDK = "use_stripe_sdk";

    @NonNull
    Map<String, Object> toParamMap();

    @NonNull
    String getClientSecret();

    boolean shouldUseStripeSdk();

    @NonNull
    ConfirmStripeIntentParams withShouldUseStripeSdk(boolean shouldUseStripeSdk);
}
