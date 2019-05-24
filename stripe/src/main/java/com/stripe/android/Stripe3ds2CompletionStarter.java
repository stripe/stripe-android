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

class Stripe3ds2CompletionStarter
        implements ActivityStarter<Stripe3ds2CompletionStarter.StartData> {
    @NonNull private final Activity mActivity;
    private final int mRequestCode;

    Stripe3ds2CompletionStarter(@NonNull Activity activity, int requestCode) {
        mActivity = activity;
        mRequestCode = requestCode;
    }

    @Override
    public void start(@NonNull StartData data) {
        final Intent intent = new Intent(mActivity, PaymentAuthRelayActivity.class)
                .putExtra(PaymentAuthenticationExtras.CLIENT_SECRET,
                        data.mPaymentIntent.getClientSecret());
        mActivity.startActivityForResult(intent, mRequestCode);
    }

    @IntDef({Status.COMPLETE, Status.CANCEL, Status.TIMEOUT, Status.PROTOCOL_ERROR,
            Status.RUNTIME_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    @interface Status {
        int COMPLETE = 0;
        int CANCEL = 1;
        int TIMEOUT = 2;
        int PROTOCOL_ERROR = 3;
        int RUNTIME_ERROR = 4;
    }

    static class StartData {
        @NonNull private final PaymentIntent mPaymentIntent;
        @Status private final int mStatus;
        @Nullable private final String mCompletionTransactionStatus;

        @NonNull
        static StartData createForComplete(@NonNull PaymentIntent paymentIntent,
                                           @NonNull String completionTransactionStatus) {
            return new StartData(paymentIntent, Status.COMPLETE, completionTransactionStatus);
        }

        StartData(@NonNull PaymentIntent paymentIntent,
                  @Status int status) {
            this(paymentIntent, status, null);
        }

        private StartData(@NonNull PaymentIntent paymentIntent, @Status int status,
                          @Nullable String completionTransactionStatus) {
            mPaymentIntent = paymentIntent;
            mStatus = status;
            mCompletionTransactionStatus = completionTransactionStatus;
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(mPaymentIntent, mStatus, mCompletionTransactionStatus);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof StartData && typedEquals((StartData) obj));
        }

        private boolean typedEquals(@NonNull StartData startData) {
            return ObjectUtils.equals(mPaymentIntent, startData.mPaymentIntent) &&
                    ObjectUtils.equals(mStatus, startData.mStatus) &&
                    ObjectUtils.equals(mCompletionTransactionStatus,
                            startData.mCompletionTransactionStatus);
        }
    }
}
