package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentAuthRelayActivity;

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
        // TODO(mshafrir) set extras
        final Intent intent = new Intent(mActivity, PaymentAuthRelayActivity.class);
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
        @Status private final int mStatus;
        @Nullable private final String mCompletionTransactionStatus;

        @NonNull
        static StartData createForComplete(@NonNull String completionTransactionStatus) {
            return new StartData(Status.COMPLETE, completionTransactionStatus);
        }

        StartData(@Status int status) {
            this(status, null);
        }

        StartData(@Status int status,
                  @Nullable String completionTransactionStatus) {
            mStatus = status;
            mCompletionTransactionStatus = completionTransactionStatus;
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(mStatus, mCompletionTransactionStatus);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof StartData && typedEquals((StartData) obj));
        }

        private boolean typedEquals(@NonNull StartData startData) {
            return ObjectUtils.equals(mStatus, startData.mStatus) &&
                    ObjectUtils.equals(mCompletionTransactionStatus,
                            startData.mCompletionTransactionStatus);
        }
    }
}
