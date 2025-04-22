package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOption

internal class FlowControllerTestFactory(
    callConfirmOnPaymentOptionCallback: Boolean,
    builder: PaymentSheet.FlowController.Builder.() -> Unit,
    configureCallbackTurbine: Turbine<PaymentOption?>,
    resultCallback: PaymentSheetResultCallback,
) {
    // Needs to be lateinit in order to reference in `paymentOptionCallback`
    private lateinit var flowController: PaymentSheet.FlowController
    private val flowControllerBuilder = PaymentSheet.FlowController.Builder(
            resultCallback = resultCallback,
            paymentOptionCallback = { paymentOption ->
                configureCallbackTurbine.add(paymentOption)
                if (callConfirmOnPaymentOptionCallback) {
                    flowController.confirm()
                }
            },
        ).apply {
            builder()
        }

    fun make(activity: ComponentActivity): PaymentSheet.FlowController {
        flowController = flowControllerBuilder.build(activity)
        return flowController
    }

    @Composable
    fun make(): PaymentSheet.FlowController {
        flowController = flowControllerBuilder.build()
        return flowController
    }
}
