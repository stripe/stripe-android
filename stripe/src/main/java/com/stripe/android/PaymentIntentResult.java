package com.stripe.android;

import androidx.annotation.NonNull;

import com.stripe.android.model.PaymentIntent;

public final class PaymentIntentResult extends StripeIntentResult<PaymentIntent> {

    private PaymentIntentResult(@NonNull Builder builder) {
        super(builder.mPaymentIntent, builder.mOutcome);
    }

    static final class Builder implements ObjectBuilder<PaymentIntentResult> {
        private PaymentIntent mPaymentIntent;
        @Outcome private int mOutcome;

        @NonNull
        Builder setPaymentIntent(@NonNull PaymentIntent paymentIntent) {
            mPaymentIntent = paymentIntent;
            return this;
        }

        @NonNull
        Builder setOutcome(@Outcome int outcome) {
            mOutcome = outcome;
            return this;
        }

        @NonNull
        public PaymentIntentResult build() {
            return new PaymentIntentResult(this);
        }
    }
}
