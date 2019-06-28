package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;

public interface StripeIntentParams {

    @NonNull
    Map<String, Object> toParamMap();

    @Nullable
    String getClientSecret();
}
