package com.stripe.android.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.FrameLayout;

import com.stripe.android.R;
import com.stripe.android.model.PaymentMethod;

@SuppressLint("ViewConstructor")
public class AddPaymentMethodCardRowView extends FrameLayout {
    AddPaymentMethodCardRowView(@NonNull final Activity activity,
                                @NonNull final PaymentMethodsActivityStarter.Args args) {
        super(activity);
        inflate(activity, R.layout.add_payment_method_card_row, this);
        setId(R.id.payment_methods_add_card);

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View view) {
                new AddPaymentMethodActivityStarter(activity)
                        .startForResult(PaymentMethodsActivity.REQUEST_CODE_ADD_PAYMENT_METHOD,
                                new AddPaymentMethodActivityStarter.Args.Builder()
                                        .setShouldUpdateCustomer(true)
                                        .setShouldRequirePostalCode(args.shouldRequirePostalCode)
                                        .setIsPaymentSessionActive(args.isPaymentSessionActive)
                                        .setPaymentMethodType(PaymentMethod.Type.Card)
                                        .setPaymentConfiguration(args.paymentConfiguration)
                                        .build());
            }
        });
    }
}
