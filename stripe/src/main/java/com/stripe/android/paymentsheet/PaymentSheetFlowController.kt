package com.stripe.android.paymentsheet

import android.content.Context
import androidx.activity.ComponentActivity
import com.stripe.android.paymentsheet.model.PaymentOption

internal interface PaymentSheetFlowController {

    fun presentPaymentOptions(
        activity: ComponentActivity,
        onComplete: (PaymentOption?) -> Unit
    )

    fun confirmPayment(
        activity: ComponentActivity,
        onComplete: (PaymentSheet.CompletionStatus) -> Unit
    )

    companion object {
        fun create(
            context: Context,
            clientSecret: String,
            ephemeralKey: String,
            customerId: String,
            onComplete: (PaymentSheetFlowController) -> Unit
        ) {
            PaymentSheetFlowControllerFactory(context).create(
                clientSecret,
                ephemeralKey,
                customerId,
                onComplete
            )
        }

        fun create(
            context: Context,
            clientSecret: String,
            onComplete: (PaymentSheetFlowController) -> Unit
        ) {
            PaymentSheetFlowControllerFactory(context).create(
                clientSecret,
                onComplete
            )
        }
    }
}
