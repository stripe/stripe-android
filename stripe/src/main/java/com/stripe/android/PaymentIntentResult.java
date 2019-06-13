package com.stripe.android;

import android.app.Activity;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A model representing the result of a payment confirmation or authentication attempt via
 * {@link Stripe#confirmPayment(Activity, PaymentIntentParams)} or
 * {@link Stripe#authenticatePayment(Activity, PaymentIntent)}}
 *
 * {@link #paymentIntent} represents a {@link PaymentIntent} retrieved after payment
 * confirmation/authentication succeeded or failed.
 */
public final class PaymentIntentResult {
    @NonNull public final PaymentIntent paymentIntent;
    @Status public final int status;

    /**
     * Values that indicate the outcome of confirmation and payment authentication.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Status.UNKNOWN, Status.SUCCEEDED, Status.FAILED, Status.CANCELED})
    public @interface Status {
        int UNKNOWN = 0;

        /**
         * Confirmation or payment authentication succeeded
         */
        int SUCCEEDED = 1;

        /**
         * Confirm or payment authentication failed
         */
        int FAILED = 2;

        /**
         * Payment authentication was canceled by the user
         */
        int CANCELED = 3;
    }

    private PaymentIntentResult(@NonNull Builder builder) {
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
        public PaymentIntentResult build() {
            return new PaymentIntentResult(this);
        }
    }
}
