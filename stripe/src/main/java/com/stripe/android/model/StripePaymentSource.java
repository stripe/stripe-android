package com.stripe.android.model;

import androidx.annotation.Nullable;

/**
 * Represents an object that has an ID field that can be used to create payments with Stripe.
 */
public interface StripePaymentSource {
    @Nullable String getId();
}
