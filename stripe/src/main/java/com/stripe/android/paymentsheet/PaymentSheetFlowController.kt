package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.stripe.android.paymentsheet.model.PaymentOption

internal interface PaymentSheetFlowController {

    fun presentPaymentOptions(
        activity: ComponentActivity,
        onComplete: (PaymentOption?) -> Unit
    )

    fun onPaymentOptionResult(intent: Intent?): PaymentOption?

    fun confirmPayment(
        activity: ComponentActivity,
        onComplete: (PaymentResult) -> Unit
    )

    sealed class Result {
        class Success(
            val paymentSheetFlowController: PaymentSheetFlowController
        ) : Result()

        class Failure(
            val error: Throwable
        ) : Result()
    }

    companion object {
        fun create(
            context: Context,
            clientSecret: String,
            ephemeralKey: String,
            customerId: String,
            googlePayConfig: PaymentSheet.GooglePayConfiguration? = null,
            onComplete: (Result) -> Unit
        ) {
            PaymentSheetFlowControllerFactory(context).create(
                clientSecret,
                ephemeralKey,
                customerId,
                googlePayConfig,
                onComplete
            )
        }

        fun create(
            context: Context,
            clientSecret: String,
            googlePayConfig: PaymentSheet.GooglePayConfiguration? = null,
            onComplete: (Result) -> Unit
        ) {
            PaymentSheetFlowControllerFactory(context).create(
                clientSecret,
                googlePayConfig,
                onComplete
            )
        }
    }
}
