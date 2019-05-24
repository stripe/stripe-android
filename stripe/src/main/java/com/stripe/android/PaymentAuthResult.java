package com.stripe.android;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;

/**
 * A model representing the result of a payment authentication attempt via
 * {@link Stripe#startPaymentAuth(Activity, PaymentIntentParams)}.
 *
 * {@link #paymentIntent} represents a {@link PaymentIntent} retrieved after payment authentication
 * succeeded or failed.
 */
public final class PaymentAuthResult {
    @NonNull public final PaymentIntent paymentIntent;

    private PaymentAuthResult(@NonNull Builder builder) {
        this.paymentIntent = builder.mPaymentIntent;
    }

    static final class Builder {
        private PaymentIntent mPaymentIntent;

        @NonNull
        public Builder setPaymentIntent(@NonNull PaymentIntent paymentIntent) {
            mPaymentIntent = paymentIntent;
            return this;
        }

        @NonNull
        public PaymentAuthResult build() {
            return new PaymentAuthResult(this);
        }
    }
}
