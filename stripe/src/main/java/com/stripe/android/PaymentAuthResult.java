package com.stripe.android;

import android.app.Activity;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A model representing the result of a payment authentication attempt via
 * {@link Stripe#startPaymentAuth(Activity, PaymentIntentParams)}.
 *
 * {@link #paymentIntent} represents a {@link PaymentIntent} retrieved after payment authentication
 * succeeded or failed.
 */
public final class PaymentAuthResult {
    @NonNull public final PaymentIntent paymentIntent;
    @Status public final int status;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Status.UNKNOWN, Status.SUCCEEDED, Status.FAILED, Status.CANCELED})
    public @interface Status {
        int UNKNOWN = 0;
        int SUCCEEDED = 1;
        int FAILED = 2;
        int CANCELED = 3;
    }

    private PaymentAuthResult(@NonNull Builder builder) {
        this.paymentIntent = builder.mPaymentIntent;
        this.status = builder.mStatus;
    }

    static final class Builder {
        private PaymentIntent mPaymentIntent;
        @Status private int mStatus;

        @NonNull
        public Builder setPaymentIntent(@NonNull PaymentIntent paymentIntent) {
            mPaymentIntent = paymentIntent;
            return this;
        }

        @NonNull
        public Builder setStatus(@Status int status) {
            mStatus = status;
            return this;
        }

        @NonNull
        public PaymentAuthResult build() {
            return new PaymentAuthResult(this);
        }
    }
}
