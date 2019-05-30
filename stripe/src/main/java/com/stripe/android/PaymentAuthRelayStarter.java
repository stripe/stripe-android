package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentAuthRelayActivity;
import com.stripe.android.view.PaymentAuthenticationExtras;

/**
 * Starts an instance of {@link PaymentAuthRelayStarter}.
 * Should only be called from {@link com.stripe.android.PaymentAuthenticationController}.
 */
class PaymentAuthRelayStarter implements ActivityStarter<PaymentAuthRelayStarter.Data> {
    @NonNull private final Activity mActivity;
    private final int mRequestCode;

    PaymentAuthRelayStarter(@NonNull Activity activity, int requestCode) {
        mActivity = activity;
        mRequestCode = requestCode;
    }

    @Override
    public void start(@NonNull Data data) {
        final Intent intent = new Intent(mActivity, PaymentAuthRelayActivity.class)
                .putExtra(PaymentAuthenticationExtras.CLIENT_SECRET,
                        data.paymentIntent != null ? data.paymentIntent.getClientSecret() : null)
                .putExtra(PaymentAuthenticationExtras.AUTH_EXCEPTION,
                        data.exception);
        mActivity.startActivityForResult(intent, mRequestCode);
    }

    public static final class Data {
        @Nullable final PaymentIntent paymentIntent;
        @Nullable final Exception exception;

        /**
         * Use when payment authentication completed or can be bypassed.
         */
        Data(@NonNull PaymentIntent paymentIntent) {
            this.paymentIntent = paymentIntent;
            this.exception = null;
        }

        /**
         * Use when payment authentication resulted in an error.
         */
        Data(@NonNull Exception exception) {
            this.paymentIntent = null;
            this.exception = exception;
        }
    }
}
