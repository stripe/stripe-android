package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import java.lang.IllegalStateException

internal object ExternalPaymentMethodInterceptor {

    var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null

    fun intercept(
        externalPaymentMethodType: String,
        billingDetails: PaymentMethod.BillingDetails?,
        onPaymentResult: (PaymentResult) -> Unit,
        externalPaymentMethodLauncher: ActivityResultLauncher<ExternalPaymentMethodInput>?,
        errorReporter: ErrorReporter,
    ) {
        val externalPaymentMethodConfirmHandler = this.externalPaymentMethodConfirmHandler
        if (externalPaymentMethodConfirmHandler == null) {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER_NULL,
                additionalNonPiiParams = mapOf("external_payment_method_type" to externalPaymentMethodType)
            )
            onPaymentResult(
                PaymentResult.Failed(
                    throwable = IllegalStateException(
                        "externalPaymentMethodConfirmHandler is null." +
                            " Cannot process payment for payment selection: $externalPaymentMethodType"
                    )
                )
            )
        } else if (externalPaymentMethodLauncher == null) {
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_LAUNCHER_NULL,
                additionalNonPiiParams = mapOf("external_payment_method_type" to externalPaymentMethodType)
            )
            onPaymentResult(
                PaymentResult.Failed(
                    throwable = IllegalStateException(
                        "externalPaymentMethodLauncher is null." +
                            " Cannot process payment for payment selection: $externalPaymentMethodType"
                    )
                )
            )
        } else {
            errorReporter.report(
                ErrorReporter.SuccessEvent.EXTERNAL_PAYMENT_METHODS_LAUNCH_SUCCESS,
                additionalNonPiiParams = mapOf("external_payment_method_type" to externalPaymentMethodType)
            )
            externalPaymentMethodLauncher.launch(
                ExternalPaymentMethodInput(
                    type = externalPaymentMethodType,
                    billingDetails = billingDetails,
                )
            )
        }
    }
}
