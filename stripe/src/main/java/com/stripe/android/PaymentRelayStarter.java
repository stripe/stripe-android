package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.utils.ObjectUtils;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentRelayActivity;
import com.stripe.android.view.PaymentResultExtras;

/**
 * Starts an instance of {@link PaymentRelayStarter}.
 * Should only be called from {@link PaymentController}.
 */
class PaymentRelayStarter implements ActivityStarter<PaymentRelayStarter.Data> {
    @NonNull private final Activity mActivity;
    private final int mRequestCode;

    PaymentRelayStarter(@NonNull Activity activity, int requestCode) {
        mActivity = activity;
        mRequestCode = requestCode;
    }

    @Override
    public void start(@NonNull Data data) {
        final Intent intent = new Intent(mActivity, PaymentRelayActivity.class)
                .putExtra(PaymentResultExtras.CLIENT_SECRET,
                        data.paymentIntent != null ? data.paymentIntent.getClientSecret() : null)
                .putExtra(PaymentResultExtras.AUTH_EXCEPTION, data.exception)
                .putExtra(PaymentResultExtras.AUTH_STATUS, data.status);
        mActivity.startActivityForResult(intent, mRequestCode);
    }

    public static final class Data {
        @Nullable final PaymentIntent paymentIntent;
        @Nullable final Exception exception;
        @PaymentIntentResult.Status final int status;

        /**
         * Use when payment authentication completed or can be bypassed.
         */
        Data(@NonNull PaymentIntent paymentIntent) {
            this.paymentIntent = paymentIntent;
            this.status = PaymentIntentResult.Status.SUCCEEDED;
            this.exception = null;
        }

        /**
         * Use when payment authentication resulted in an error.
         */
        Data(@NonNull Exception exception) {
            this.paymentIntent = null;
            this.status = PaymentIntentResult.Status.FAILED;
            this.exception = exception;
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(paymentIntent, exception, status);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Data && typedEquals((Data) obj));
        }

        private boolean typedEquals(@NonNull Data data) {
            return ObjectUtils.equals(paymentIntent, data.paymentIntent) &&
                    ObjectUtils.equals(exception, data.exception) &&
                    ObjectUtils.equals(status, data.status);
        }
    }
}
