package com.stripe.android.elements

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

/**
 * Entry point class that's held to control the PaymentElement.
 */
interface PaymentElementController {

    data class Config(
        val paymentSheetConfig: PaymentSheet.Configuration?,
        val stripeIntent: StripeIntent,
        val merchantName: String,
        val initialSelection: PaymentSelection.New?
    )
}
