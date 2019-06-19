package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.StripeIntent;
import com.stripe.android.utils.ObjectUtils;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentRelayActivity;
import com.stripe.android.view.StripeIntentResultExtras;

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
                .putExtra(StripeIntentResultExtras.CLIENT_SECRET,
                        data.stripeIntent != null ? data.stripeIntent.getClientSecret() : null)
                .putExtra(StripeIntentResultExtras.AUTH_EXCEPTION, data.exception)
                .putExtra(StripeIntentResultExtras.AUTH_STATUS, data.status);
        mActivity.startActivityForResult(intent, mRequestCode);
    }

    public static final class Data {
        @Nullable final StripeIntent stripeIntent;
        @Nullable final Exception exception;
        @StripeIntentResult.Status final int status;

        /**
         * Use when payment authentication completed or can be bypassed.
         */
        Data(@NonNull StripeIntent stripeIntent) {
            this.stripeIntent = stripeIntent;
            this.status = StripeIntentResult.Status.SUCCEEDED;
            this.exception = null;
        }

        /**
         * Use when payment authentication resulted in an error.
         */
        Data(@NonNull Exception exception) {
            this.stripeIntent = null;
            this.status = StripeIntentResult.Status.FAILED;
            this.exception = exception;
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(stripeIntent, exception, status);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Data && typedEquals((Data) obj));
        }

        private boolean typedEquals(@NonNull Data data) {
            return ObjectUtils.equals(stripeIntent, data.stripeIntent) &&
                    ObjectUtils.equals(exception, data.exception) &&
                    ObjectUtils.equals(status, data.status);
        }
    }
}
