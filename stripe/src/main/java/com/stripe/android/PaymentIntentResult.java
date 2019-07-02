package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.model.PaymentIntent;

public final class PaymentIntentResult implements StripeIntentResult<PaymentIntent> {
    @NonNull private final PaymentIntent paymentIntent;
    @Status private final int status;

    private PaymentIntentResult(@NonNull Builder builder) {
        this.paymentIntent = builder.mPaymentIntent;
        this.status = builder.mStatus;
    }

    @NonNull
    @Override
    public PaymentIntent getIntent() {
        return paymentIntent;
    }

    @Override
    public int getStatus() {
        return status;
    }

    static final class Builder implements ObjectBuilder<PaymentIntentResult> {
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
