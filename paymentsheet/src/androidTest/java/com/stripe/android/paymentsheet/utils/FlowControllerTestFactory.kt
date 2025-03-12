package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.rememberPaymentSheetFlowController

internal class FlowControllerTestFactory(
    private val callConfirmOnPaymentOptionCallback: Boolean,
    private val createIntentCallback: CreateIntentCallback? = null,
    private val configureCallbackTurbine: Turbine<PaymentOption?>,
    private val resultCallback: PaymentSheetResultCallback,
) {

    fun make(activity: ComponentActivity): PaymentSheet.FlowController {
        // Needs to be lateinit in order to reference in `paymentOptionCallback`
        lateinit var flowController: PaymentSheet.FlowController
        flowController = if (createIntentCallback != null) {
            PaymentSheet.FlowController.create(
                activity = activity,
                paymentOptionCallback = { paymentOption ->
                    configureCallbackTurbine.add(paymentOption)
                    if (callConfirmOnPaymentOptionCallback) {
                        flowController.confirm()
                    }
                },
                createIntentCallback = createIntentCallback,
                paymentResultCallback = resultCallback,
            )
        } else {
            PaymentSheet.FlowController.create(
                activity = activity,
                paymentOptionCallback = { paymentOption ->
                    configureCallbackTurbine.add(paymentOption)
                    if (callConfirmOnPaymentOptionCallback) {
                        flowController.confirm()
                    }
                },
                paymentResultCallback = resultCallback,
            )
        }
        return flowController
    }

    @Composable
    fun make(): PaymentSheet.FlowController {
        // Needs to be lateinit in order to reference in `paymentOptionCallback`
        lateinit var flowController: PaymentSheet.FlowController
        flowController = if (createIntentCallback != null) {
            rememberPaymentSheetFlowController(
                createIntentCallback = createIntentCallback,
                paymentOptionCallback = { paymentOption ->
                    configureCallbackTurbine.add(paymentOption)
                    if (callConfirmOnPaymentOptionCallback) {
                        flowController.confirm()
                    }
                },
                paymentResultCallback = resultCallback,
            )
        } else {
            rememberPaymentSheetFlowController(
                paymentOptionCallback = { paymentOption ->
                    configureCallbackTurbine.add(paymentOption)
                    if (callConfirmOnPaymentOptionCallback) {
                        flowController.confirm()
                    }
                },
                paymentResultCallback = resultCallback,
            )
        }
        return flowController
    }
}
