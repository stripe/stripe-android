package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;

/**
 * Represents a JSON model used in the Stripe Api.
 */
public abstract class StripeModel {

    @NonNull
    public abstract Map<String, Object> toMap();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(@Nullable Object obj);
}
