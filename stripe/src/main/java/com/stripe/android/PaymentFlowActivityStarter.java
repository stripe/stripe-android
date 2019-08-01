package com.stripe.android;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentFlowActivity;

final class PaymentFlowActivityStarter extends ActivityStarter<PaymentFlowActivity> {
    PaymentFlowActivityStarter(@NonNull Activity activity) {
        super(activity, PaymentFlowActivity.class);
    }

    PaymentFlowActivityStarter(@NonNull Fragment fragment) {
        super(fragment, PaymentFlowActivity.class);
    }
}
