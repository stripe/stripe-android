package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.utils.ObjectUtils;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentAuthRelayActivity;
import com.stripe.android.view.PaymentAuthenticationExtras;

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

        final Intent intent = new Intent(activity, PaymentAuthRelayActivity.class)
                .putExtra(PaymentAuthenticationExtras.CLIENT_SECRET,
                        data.mPaymentIntent.getClientSecret())
                .putExtra(PaymentAuthenticationExtras.AUTH_STATUS,
                        data.getAuthStatus());
        activity.startActivityForResult(intent, mRequestCode);
    }

    @IntDef({ChallengeFlowStatus.COMPLETE, ChallengeFlowStatus.CANCEL, ChallengeFlowStatus.TIMEOUT,
            ChallengeFlowStatus.PROTOCOL_ERROR, ChallengeFlowStatus.RUNTIME_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    @interface ChallengeFlowStatus {
        int COMPLETE = 0;
        int CANCEL = 1;
        int TIMEOUT = 2;
        int PROTOCOL_ERROR = 3;
        int RUNTIME_ERROR = 4;
    }

    static class StartData {
        @NonNull private final PaymentIntent mPaymentIntent;
        @ChallengeFlowStatus private final int mChallengeFlowStatus;
        @Nullable private final String mCompletionTransactionStatus;

        @NonNull
        static StartData createForComplete(@NonNull PaymentIntent paymentIntent,
                                           @NonNull String completionTransactionStatus) {
            return new StartData(paymentIntent, ChallengeFlowStatus.COMPLETE,
                    completionTransactionStatus);
        }

        StartData(@NonNull PaymentIntent paymentIntent,
                  @ChallengeFlowStatus int status) {
            this(paymentIntent, status, null);
        }

        private StartData(@NonNull PaymentIntent paymentIntent,
                          @ChallengeFlowStatus int challengeFlowStatus,
                          @Nullable String completionTransactionStatus) {
            mPaymentIntent = paymentIntent;
            mChallengeFlowStatus = challengeFlowStatus;
            mCompletionTransactionStatus = completionTransactionStatus;
        }

        @PaymentAuthResult.Status
        private int getAuthStatus() {
            if (mChallengeFlowStatus == ChallengeFlowStatus.COMPLETE) {
                return PaymentAuthResult.Status.SUCCEEDED;
            } else if (mChallengeFlowStatus == ChallengeFlowStatus.CANCEL) {
                return PaymentAuthResult.Status.CANCELED;
            } else {
                return PaymentAuthResult.Status.FAILED;
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
