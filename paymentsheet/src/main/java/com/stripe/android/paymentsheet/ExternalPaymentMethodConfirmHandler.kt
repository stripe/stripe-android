package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent

interface ExternalPaymentMethodHandler {
    // TODO: add billing details
    fun createIntent(context: Context, externalPaymentMethodType: String, billingDetails: PaymentSheet.BillingDetails,): Intent
}
