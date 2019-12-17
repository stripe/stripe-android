package com.stripe.android.view

import android.annotation.SuppressLint
import android.app.Activity
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod

@SuppressLint("ViewConstructor")
internal class AddPaymentMethodCardRowView internal constructor(
    activity: Activity,
    args: PaymentMethodsActivityStarter.Args
) : AddPaymentMethodRowView(
    activity,
    R.layout.add_payment_method_card_row,
    R.id.stripe_payment_methods_add_card,
    AddPaymentMethodActivityStarter.Args.Builder()
        .setBillingAddressFields(args.billingAddressFields)
        .setShouldAttachToCustomer(true)
        .setIsPaymentSessionActive(args.isPaymentSessionActive)
        .setPaymentMethodType(PaymentMethod.Type.Card)
        .setAddPaymentMethodFooter(args.addPaymentMethodFooterLayoutId)
        .setPaymentConfiguration(args.paymentConfiguration)
        .setWindowFlags(args.windowFlags)
        .build()
)
