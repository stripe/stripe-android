package com.stripe.android.paymentsheet

import androidx.activity.ComponentActivity
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentOption

internal class DefaultPaymentSheetFlowController internal constructor(
    private val args: PaymentSheetActivityStarter.Args,
    private val paymentMethods: List<PaymentMethod>,
    private val defaultPaymentMethodId: String?
) : PaymentSheetFlowController {

    override fun presentPaymentOptions(
        activity: ComponentActivity,
        onComplete: (PaymentOption?) -> Unit
    ) {
        // TODO(mshafrir-stripe): implement

        onComplete(null)
    }

    override fun confirmPayment(
        activity: ComponentActivity,
        onComplete: (PaymentResult) -> Unit
    ) {
        // TODO(mshafrir-stripe): implement

        onComplete(
            PaymentResult.Cancelled(null, null)
        )
    }
}
