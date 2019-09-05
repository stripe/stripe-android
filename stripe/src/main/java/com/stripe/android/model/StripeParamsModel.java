package com.stripe.android.model;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * Model for a Stripe API object creation parameters
 */
public interface StripeParamsModel {

    @NonNull
    Map<String, Object> toParamMap();
}
