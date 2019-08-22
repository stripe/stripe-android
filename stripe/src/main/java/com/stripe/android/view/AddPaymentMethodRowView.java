package com.stripe.android.view;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.FrameLayout;

abstract class AddPaymentMethodRowView extends FrameLayout {
    AddPaymentMethodRowView(@NonNull final Activity activity,
                            @LayoutRes int layoutId,
                            @IdRes int idRes,
                            @NonNull final AddPaymentMethodActivityStarter.Args args) {
        super(activity);
        inflate(activity, layoutId, this);
        setId(idRes);

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View view) {
                new AddPaymentMethodActivityStarter(activity).startForResult(args);
            }
        });
    }
}
