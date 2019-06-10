package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.utils.ObjectUtils;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentRelayActivity;
import com.stripe.android.view.PaymentResultExtras;

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
                .putExtra(PaymentResultExtras.CLIENT_SECRET,
                        data.mPaymentIntent.getClientSecret())
                .putExtra(PaymentResultExtras.AUTH_STATUS,
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
        @NonNull private final PaymentIntent mPaymentIntent;
        @ChallengeFlowOutcome private final int mChallengeFlowStatus;
        @Nullable private final String mCompletionTransactionStatus;

        @NonNull
        static StartData createForComplete(@NonNull PaymentIntent paymentIntent,
                                           @NonNull String completionTransactionStatus) {
            return new StartData(paymentIntent, ChallengeFlowOutcome.COMPLETE,
                    completionTransactionStatus);
        }

        StartData(@NonNull PaymentIntent paymentIntent,
                  @ChallengeFlowOutcome int status) {
            this(paymentIntent, status, null);
        }

        private StartData(@NonNull PaymentIntent paymentIntent,
                          @ChallengeFlowOutcome int challengeFlowStatus,
                          @Nullable String completionTransactionStatus) {
            mPaymentIntent = paymentIntent;
            mChallengeFlowStatus = challengeFlowStatus;
            mCompletionTransactionStatus = completionTransactionStatus;
        }

        @PaymentIntentResult.Status
        private int getAuthStatus() {
            if (mChallengeFlowStatus == ChallengeFlowOutcome.COMPLETE) {
                return PaymentIntentResult.Status.SUCCEEDED;
            } else if (mChallengeFlowStatus == ChallengeFlowOutcome.CANCEL) {
                return PaymentIntentResult.Status.CANCELED;
            } else {
                return PaymentIntentResult.Status.FAILED;
            }
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(mPaymentIntent, mChallengeFlowStatus,
                    mCompletionTransactionStatus);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof StartData && typedEquals((StartData) obj));
        }

        private boolean typedEquals(@NonNull StartData startData) {
            return ObjectUtils.equals(mPaymentIntent, startData.mPaymentIntent) &&
                    ObjectUtils.equals(mChallengeFlowStatus, startData.mChallengeFlowStatus) &&
                    ObjectUtils.equals(mCompletionTransactionStatus,
                            startData.mCompletionTransactionStatus);
        }
    }
}
