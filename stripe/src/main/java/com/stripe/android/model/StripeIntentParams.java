package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;

public interface StripeIntentParams {

    String API_PARAM_CLIENT_SECRET = "client_secret";
    String API_PARAM_RETURN_URL = "return_url";
    String API_PARAM_PAYMENT_METHOD_ID = "payment_method";

    @NonNull
    Map<String, Object> toParamMap();

    @NonNull
    String getClientSecret();
}
