package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

public class PaymentMethodsActivityStarter {

    @NonNull private final Activity mActivity;

    public PaymentMethodsActivityStarter(@NonNull Activity activity) {
        mActivity = activity;
    }

    public void startForResult(final int requestCode) {
        mActivity.startActivityForResult(newIntent(), requestCode);
    }

    @NonNull
    public Intent newIntent() {
        return new Intent(mActivity, PaymentMethodsActivity.class);
    }
}
