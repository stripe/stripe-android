package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.StripeIntent;
import com.stripe.android.utils.ObjectUtils;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentRelayActivity;
import com.stripe.android.view.StripeIntentResultExtras;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

class Stripe3ds2CompletionStarter
        implements ActivityStarter<Stripe3ds2CompletionStarter.StartData> {
    @NonNull private final WeakReference<Activity> mActivityRef;
    private final int mRequestCode;

    Stripe3ds2CompletionStarter(@NonNull Activity activity, int requestCode) {
        mActivityRef = new WeakReference<>(activity);
        mRequestCode = requestCode;
    }

    @Override
    public void start(@NonNull StartData data) {
        final Activity activity = mActivityRef.get();
        if (activity == null) {
            return;
        }

        final Intent intent = new Intent(activity, PaymentRelayActivity.class)
                .putExtra(StripeIntentResultExtras.CLIENT_SECRET,
                        data.mStripeIntent.getClientSecret())
                .putExtra(StripeIntentResultExtras.AUTH_STATUS,
                        data.getAuthStatus());
        activity.startActivityForResult(intent, mRequestCode);
    }

    @IntDef({ChallengeFlowOutcome.COMPLETE, ChallengeFlowOutcome.CANCEL,
            ChallengeFlowOutcome.TIMEOUT, ChallengeFlowOutcome.PROTOCOL_ERROR,
            ChallengeFlowOutcome.RUNTIME_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    @interface ChallengeFlowOutcome {
        int COMPLETE = 0;
        int CANCEL = 1;
        int TIMEOUT = 2;
        int PROTOCOL_ERROR = 3;
        int RUNTIME_ERROR = 4;
    }

    static class StartData {
        @NonNull private final StripeIntent mStripeIntent;
        @ChallengeFlowOutcome private final int mChallengeFlowStatus;
        @Nullable private final String mCompletionTransactionStatus;

        @NonNull
        static StartData createForComplete(@NonNull StripeIntent stripeIntent,
                                           @NonNull String completionTransactionStatus) {
            return new StartData(stripeIntent, ChallengeFlowOutcome.COMPLETE,
                    completionTransactionStatus);
        }

        StartData(@NonNull StripeIntent stripeIntent,
                  @ChallengeFlowOutcome int status) {
            this(stripeIntent, status, null);
        }

        private StartData(@NonNull StripeIntent stripeIntent,
                          @ChallengeFlowOutcome int challengeFlowStatus,
                          @Nullable String completionTransactionStatus) {
            mStripeIntent = stripeIntent;
            mChallengeFlowStatus = challengeFlowStatus;
            mCompletionTransactionStatus = completionTransactionStatus;
        }

        @StripeIntentResult.Status
        private int getAuthStatus() {
            if (mChallengeFlowStatus == ChallengeFlowOutcome.COMPLETE) {
                return StripeIntentResult.Status.SUCCEEDED;
            } else if (mChallengeFlowStatus == ChallengeFlowOutcome.CANCEL) {
                return StripeIntentResult.Status.CANCELED;
            } else {
                return StripeIntentResult.Status.FAILED;
            }
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(mStripeIntent, mChallengeFlowStatus,
                    mCompletionTransactionStatus);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof StartData && typedEquals((StartData) obj));
        }

        private boolean typedEquals(@NonNull StartData startData) {
            return ObjectUtils.equals(mStripeIntent, startData.mStripeIntent) &&
                    ObjectUtils.equals(mChallengeFlowStatus, startData.mChallengeFlowStatus) &&
                    ObjectUtils.equals(mCompletionTransactionStatus,
                            startData.mCompletionTransactionStatus);
        }
    }
}
