package com.stripe.android;

import androidx.annotation.NonNull;

import com.stripe.android.model.SetupIntent;

public class SetupIntentResult extends StripeIntentResult<SetupIntent> {

    private SetupIntentResult(@NonNull Builder builder) {
        super(builder.mSetupIntent, builder.mOutcome);
    }

    static final class Builder implements ObjectBuilder<SetupIntentResult> {
        private SetupIntent mSetupIntent;
        @Outcome private int mOutcome;

        @NonNull
        Builder setSetupIntent(@NonNull SetupIntent setupIntent) {
            mSetupIntent = setupIntent;
            return this;
        }

        @NonNull
        Builder setOutcome(@Outcome int outcome) {
            mOutcome = outcome;
            return this;
        }

        @NonNull
        public SetupIntentResult build() {
            return new SetupIntentResult(this);
        }
    }
}
