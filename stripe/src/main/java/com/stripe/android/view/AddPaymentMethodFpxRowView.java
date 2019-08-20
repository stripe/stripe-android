package com.stripe.android.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;

import com.stripe.android.R;
import com.stripe.android.model.PaymentMethod;

@SuppressLint("ViewConstructor")
public class AddPaymentMethodFpxRowView extends AddPaymentMethodRowView {
    AddPaymentMethodFpxRowView(@NonNull final Activity activity,
                               @NonNull final PaymentMethodsActivityStarter.Args args) {
        super(activity,
                R.layout.add_payment_method_fpx_row,
                R.id.payment_methods_add_fpx,
                new AddPaymentMethodActivityStarter.Args.Builder()
                        .setIsPaymentSessionActive(args.isPaymentSessionActive)
                        .setPaymentMethodType(PaymentMethod.Type.Fpx)
                        .setPaymentConfiguration(args.paymentConfiguration)
                        .build());
    }
}
