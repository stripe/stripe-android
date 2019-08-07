package com.stripe.android.model;

import android.support.annotation.NonNull;

import java.util.Map;

/**
 * Model for a Stripe API object creation parameters
 */
public interface StripeParamsModel {

    @NonNull
    Map<String, ?> toParamMap();
}
