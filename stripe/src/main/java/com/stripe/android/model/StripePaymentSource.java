package com.stripe.android.model;

/**
 * Represents an object that has an ID field that can be used to create payments with Stripe.
 */
public interface StripePaymentSource {
    String getId();
}
