package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.model.PaymentIntent;

public final class PaymentIntentResult extends StripeIntentResult<PaymentIntent> {

    private PaymentIntentResult(@NonNull Builder builder) {
        super(builder.mPaymentIntent, builder.mStatus);
    }

    static final class Builder implements ObjectBuilder<PaymentIntentResult> {
        private PaymentIntent mPaymentIntent;
        @Status private int mStatus;

        @NonNull
        Builder setPaymentIntent(@NonNull PaymentIntent paymentIntent) {
            mPaymentIntent = paymentIntent;
            return this;
        }

        @NonNull
        Builder setStatus(@Status int status) {
            mStatus = status;
            return this;
        }

        @NonNull
        public PaymentIntentResult build() {
            return new PaymentIntentResult(this);
        }
    }
}
