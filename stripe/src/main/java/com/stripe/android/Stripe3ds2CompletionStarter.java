package com.stripe.android;

import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.StripeIntent;
import com.stripe.android.utils.ObjectUtils;
import com.stripe.android.view.AuthActivityStarter;
import com.stripe.android.view.PaymentRelayActivity;
import com.stripe.android.view.StripeIntentResultExtras;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class Stripe3ds2CompletionStarter
        implements AuthActivityStarter<Stripe3ds2CompletionStarter.StartData> {
    @NonNull private final Host mHost;
    private final int mRequestCode;

    Stripe3ds2CompletionStarter(@NonNull Host host, int requestCode) {
        mHost = host;
        mRequestCode = requestCode;
    }

    @Override
    public void start(@NonNull StartData data) {
        final Bundle extras = new Bundle();
        extras.putString(StripeIntentResultExtras.CLIENT_SECRET,
                data.mStripeIntent.getClientSecret());
        extras.putInt(StripeIntentResultExtras.FLOW_OUTCOME, data.getOutcome());
        mHost.startActivityForResult(PaymentRelayActivity.class, extras, mRequestCode);
    }

    @IntDef({ChallengeFlowOutcome.COMPLETE_SUCCESSFUL, ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL,
            ChallengeFlowOutcome.CANCEL, ChallengeFlowOutcome.TIMEOUT,
            ChallengeFlowOutcome.PROTOCOL_ERROR, ChallengeFlowOutcome.RUNTIME_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    @interface ChallengeFlowOutcome {
        int COMPLETE_SUCCESSFUL = 0;
        int COMPLETE_UNSUCCESSFUL = 1;
        int CANCEL = 2;
        int TIMEOUT = 3;
        int PROTOCOL_ERROR = 4;
        int RUNTIME_ERROR = 5;
    }

    static class StartData {
        @NonNull private final StripeIntent mStripeIntent;
        @ChallengeFlowOutcome private final int mChallengeFlowOutcome;

        StartData(@NonNull StripeIntent stripeIntent,
                  @ChallengeFlowOutcome int challengeFlowOutcome) {
            mStripeIntent = stripeIntent;
            mChallengeFlowOutcome = challengeFlowOutcome;
        }

        @StripeIntentResult.Outcome
        private int getOutcome() {
            if (mChallengeFlowOutcome == ChallengeFlowOutcome.COMPLETE_SUCCESSFUL) {
                return StripeIntentResult.Outcome.SUCCEEDED;
            } else if (mChallengeFlowOutcome == ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL) {
                return StripeIntentResult.Outcome.FAILED;
            } else if (mChallengeFlowOutcome == ChallengeFlowOutcome.CANCEL) {
                return StripeIntentResult.Outcome.CANCELED;
            } else if (mChallengeFlowOutcome == ChallengeFlowOutcome.TIMEOUT) {
                return StripeIntentResult.Outcome.TIMEDOUT;
            } else {
                return StripeIntentResult.Outcome.FAILED;
            }
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(mStripeIntent, mChallengeFlowOutcome);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof StartData && typedEquals((StartData) obj));
        }

        private boolean typedEquals(@NonNull StartData startData) {
            return ObjectUtils.equals(mStripeIntent, startData.mStripeIntent) &&
                    ObjectUtils.equals(mChallengeFlowOutcome, startData.mChallengeFlowOutcome);
        }
    }
}
