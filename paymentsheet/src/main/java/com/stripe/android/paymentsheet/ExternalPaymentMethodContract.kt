package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.paymentlauncher.PaymentResult
import java.lang.IllegalArgumentException

internal class ExternalPaymentMethodContract : ActivityResultContract<ExternalPaymentMethodInput, PaymentResult>() {
    override fun createIntent(context: Context, input: ExternalPaymentMethodInput): Intent {
        return input.externalPaymentMethodConfirmHandler.createIntent(
            context = context,
            externalPaymentMethodType = input.type,
            billingDetails = input.billingDetails,
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        return when (resultCode) {
            ExternalPaymentMethodResult.Completed.resultCode -> PaymentResult.Completed
            ExternalPaymentMethodResult.Canceled.resultCode -> PaymentResult.Canceled
            ExternalPaymentMethodResult.Failed.resultCode ->
                PaymentResult.Failed(
                    throwable = Throwable(
                        cause = null,
                        message = intent?.getStringExtra(ExternalPaymentMethodResult.Failed.ERROR_MESSAGE_EXTRA)
                    )
                )

            else ->
                PaymentResult.Failed(
                    throwable = IllegalArgumentException(
                        "Invalid result code returned by external payment method activity"
                    )
                )
        }
    }
}

internal data class ExternalPaymentMethodInput(
    val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
    val type: String,
    val billingDetails: PaymentMethod.BillingDetails?,
)
