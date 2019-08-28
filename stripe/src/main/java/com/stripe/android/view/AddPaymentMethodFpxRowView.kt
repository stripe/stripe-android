package com.stripe.android.view

import android.annotation.SuppressLint
import android.app.Activity
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod

@SuppressLint("ViewConstructor")
internal class AddPaymentMethodFpxRowView internal constructor(
    activity: Activity,
    args: PaymentMethodsActivityStarter.Args
) : AddPaymentMethodRowView(
    activity,
    R.layout.add_payment_method_fpx_row,
    R.id.payment_methods_add_fpx,
    AddPaymentMethodActivityStarter.Args.Builder()
        .setIsPaymentSessionActive(args.isPaymentSessionActive)
        .setPaymentMethodType(PaymentMethod.Type.Fpx)
        .setPaymentConfiguration(args.paymentConfiguration)
        .build()
)
