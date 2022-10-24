package com.stripe.android.elements

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal data class PaymentElementConfig(
    val paymentSheetConfig: PaymentSheet.Configuration?,
    val stripeIntent: StripeIntent,
    val merchantName: String,
    val initialSelection: PaymentSelection.New?
)
