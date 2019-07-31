package com.stripe.android.view;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

public final class PaymentMethodsActivityStarter
        extends ActivityStarter<PaymentMethodsActivity> {
    public PaymentMethodsActivityStarter(@NonNull Activity activity) {
        super(activity, PaymentMethodsActivity.class);
    }

    public PaymentMethodsActivityStarter(@NonNull Fragment fragment) {
        super(fragment, PaymentMethodsActivity.class);
    }
}
