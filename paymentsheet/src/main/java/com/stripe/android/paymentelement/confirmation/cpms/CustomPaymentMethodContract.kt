package com.stripe.android.paymentelement.confirmation.cpms

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.paymentelement.CustomPaymentMethodResult
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import java.lang.IllegalArgumentException

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal class CustomPaymentMethodContract(val errorReporter: ErrorReporter) :
    ActivityResultContract<CustomPaymentMethodInput, PaymentResult>() {
    override fun createIntent(context: Context, input: CustomPaymentMethodInput): Intent {
        return Intent().setClass(
            context,
            CustomPaymentMethodProxyActivity::class.java
        )
            .putExtra(CustomPaymentMethodProxyActivity.EXTRA_CUSTOM_PAYMENT_METHOD_TYPE, input.type)
            .putExtra(CustomPaymentMethodProxyActivity.EXTRA_BILLING_DETAILS, input.billingDetails)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        return when (resultCode) {
            CustomPaymentMethodResult.Completed.RESULT_CODE -> PaymentResult.Completed
            CustomPaymentMethodResult.Canceled.RESULT_CODE -> PaymentResult.Canceled
            CustomPaymentMethodResult.Failed.RESULT_CODE ->
                PaymentResult.Failed(
                    throwable = LocalStripeException(
                        displayMessage = intent?.getStringExtra(
                            CustomPaymentMethodResult.Failed.DISPLAY_MESSAGE_EXTRA
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
