package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import app.cash.turbine.Turbine
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.FlowController.PaymentOptionDisplayData
import com.stripe.android.elements.payment.PaymentSheet

internal class FlowControllerTestFactory(
    callConfirmOnPaymentOptionCallback: Boolean,
    builder: FlowController.Builder.() -> Unit,
    configureCallbackTurbine: Turbine<PaymentOptionDisplayData?>,
    resultCallback: PaymentSheet.ResultCallback,
) {
    // Needs to be lateinit in order to reference in `paymentOptionCallback`
    private lateinit var flowController: FlowController
    private val flowControllerBuilder = FlowController.Builder(
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

    fun make(activity: ComponentActivity): FlowController {
        flowController = flowControllerBuilder.build(activity)
        return flowController
    }

    @Composable
    fun make(): FlowController {
        flowController = flowControllerBuilder.build()
        return flowController
    }
}
