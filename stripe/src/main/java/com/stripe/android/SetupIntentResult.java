package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.model.SetupIntent;

public class SetupIntentResult extends StripeIntentResult<SetupIntent> {

    private SetupIntentResult(@NonNull Builder builder) {
        super(builder.mSetupIntent, builder.mStatus);
    }

    static final class Builder implements ObjectBuilder<SetupIntentResult> {
        private SetupIntent mSetupIntent;
        @Status private int mStatus;

        @NonNull
        Builder setSetupIntent(@NonNull SetupIntent setupIntent) {
            mSetupIntent = setupIntent;
            return this;
        }

        @NonNull
        Builder setStatus(@Status int status) {
            mStatus = status;
            return this;
        }

        @NonNull
        public SetupIntentResult build() {
            return new SetupIntentResult(this);
        }
    }
}
