package com.stripe.android.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;

import com.stripe.android.R;
import com.stripe.android.model.PaymentMethod;

@SuppressLint("ViewConstructor")
public class AddPaymentMethodCardRowView extends AddPaymentMethodRowView {
    AddPaymentMethodCardRowView(@NonNull final Activity activity,
                                @NonNull final PaymentMethodsActivityStarter.Args args) {
        super(activity,
                R.layout.add_payment_method_card_row,
                R.id.payment_methods_add_card,
                new AddPaymentMethodActivityStarter.Args.Builder()
                        .setShouldAttachToCustomer(true)
                        .setShouldRequirePostalCode(args.shouldRequirePostalCode)
                        .setIsPaymentSessionActive(args.isPaymentSessionActive)
                        .setPaymentMethodType(PaymentMethod.Type.Card)
                        .setPaymentConfiguration(args.paymentConfiguration)
                        .build());
    }
}
