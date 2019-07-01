package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.model.SetupIntent;
import com.stripe.android.model.StripeIntent;

public class SetupIntentResult implements StripeIntentResult<SetupIntent> {
    @NonNull private final SetupIntent mSetupIntent;
    @Status private final int mStatus;

    private SetupIntentResult(@NonNull Builder builder) {
        mSetupIntent = builder.mSetupIntent;
        mStatus = builder.mStatus;
    }

    @NonNull
    @Override
    public SetupIntent getIntent() {
        return mSetupIntent;
    }

    @Override
    public int getStatus() {
        return mStatus;
    }

    static final class Builder<T extends StripeIntent>
            implements ObjectBuilder<SetupIntentResult> {
        private SetupIntent mSetupIntent;
        @Status private int mStatus;

        @NonNull
        public Builder setSetupIntent(@NonNull SetupIntent setupIntent) {
            mSetupIntent = setupIntent;
            return this;
        }

        @NonNull
        public Builder setStatus(@Status int status) {
            mStatus = status;
            return this;
        }

        @NonNull
        public SetupIntentResult build() {
            return new SetupIntentResult(this);
        }
    }
}
