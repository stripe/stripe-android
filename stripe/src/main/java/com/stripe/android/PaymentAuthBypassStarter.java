package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.stripe.android.model.PaymentIntent;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentAuthBypassActivity;
import com.stripe.android.view.PaymentAuthenticationExtras;

/**
 * Starts an instance of {@link PaymentAuthBypassStarter}.
 * Should only be called from {@link com.stripe.android.PaymentAuthenticationController}.
 */
class PaymentAuthBypassStarter implements ActivityStarter<PaymentIntent> {
    @NonNull private final Activity mActivity;
    private final int mRequestCode;

    PaymentAuthBypassStarter(@NonNull Activity activity, int requestCode) {
        mActivity = activity;
        mRequestCode = requestCode;
    }

    @Override
    public void start(@NonNull PaymentIntent paymentIntent) {
        final Intent intent = new Intent(mActivity, PaymentAuthBypassActivity.class)
                .putExtra(PaymentAuthenticationExtras.CLIENT_SECRET,
                        paymentIntent.getClientSecret());
        mActivity.startActivityForResult(intent, mRequestCode);
    }
}
