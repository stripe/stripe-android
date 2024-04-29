package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import java.lang.IllegalStateException

internal object ExternalPaymentMethodInterceptor {

    var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null

    fun intercept(
        externalPaymentMethodType: String,
        onPaymentResult: (PaymentResult) -> Unit,
        externalPaymentMethodLauncher: ActivityResultLauncher<ExternalPaymentMethodInput>?
    ) {
        val externalPaymentMethodConfirmHandler = this.externalPaymentMethodConfirmHandler
        if (externalPaymentMethodConfirmHandler == null) {
            onPaymentResult(
                PaymentResult.Failed(
                    throwable = IllegalStateException(
                        "externalPaymentMethodConfirmHandler is null." +
                            " Cannot process payment for payment selection: $externalPaymentMethodType"
                    )
                )
            )
        } else if (externalPaymentMethodLauncher == null) {
            onPaymentResult(
                PaymentResult.Failed(
                    throwable = IllegalStateException(
                        "externalPaymentMethodLauncher is null." +
                            " Cannot process payment for payment selection: $externalPaymentMethodType"
                    )
                )
            )
        } else {
            externalPaymentMethodLauncher.launch(
                ExternalPaymentMethodInput(
                    type = externalPaymentMethodType,
                    externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler
                )
            )
        }
    }
}
