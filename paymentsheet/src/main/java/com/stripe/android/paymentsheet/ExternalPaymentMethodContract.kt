package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import java.lang.IllegalArgumentException

internal class ExternalPaymentMethodContract(val errorReporter: ErrorReporter) :
    ActivityResultContract<ExternalPaymentMethodInput, PaymentResult>() {
    override fun createIntent(context: Context, input: ExternalPaymentMethodInput): Intent {
        return Intent().setClass(
            context,
            ExternalPaymentMethodProxyActivity::class.java
        )
            .putExtra(ExternalPaymentMethodProxyActivity.EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE, input.type)
            .putExtra(ExternalPaymentMethodProxyActivity.EXTRA_PAYMENT_ELEMENT_IDENTIFIER, input.instanceId)
            .putExtra(ExternalPaymentMethodProxyActivity.EXTRA_BILLING_DETAILS, input.billingDetails)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        return when (resultCode) {
            ExternalPaymentMethodResult.Completed.RESULT_CODE -> PaymentResult.Completed
            ExternalPaymentMethodResult.Canceled.RESULT_CODE -> PaymentResult.Canceled
            ExternalPaymentMethodResult.Failed.RESULT_CODE ->
                PaymentResult.Failed(
                    throwable = LocalStripeException(
                        displayMessage = intent?.getStringExtra(
                            ExternalPaymentMethodResult.Failed.DISPLAY_MESSAGE_EXTRA
                        ),
                        analyticsValue = "externalPaymentMethodFailure"
                    )
                )

            else -> {
                errorReporter.report(
                    ErrorReporter.UnexpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_UNEXPECTED_RESULT_CODE,
                    additionalNonPiiParams = mapOf("result_code" to resultCode.toString())
                )
                PaymentResult.Failed(
                    throwable = IllegalArgumentException(
                        "Invalid result code returned by external payment method activity"
                    )
                )
            }
        }
    }
}

internal data class ExternalPaymentMethodInput(
    val instanceId: String,
    val type: String,
    val billingDetails: PaymentMethod.BillingDetails?,
)
