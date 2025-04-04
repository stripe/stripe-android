package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import app.cash.turbine.Turbine
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.model.PaymentOption

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
internal class FlowControllerTestFactory(
    callConfirmOnPaymentOptionCallback: Boolean,
    createIntentCallback: CreateIntentCallback? = null,
    analyticEventCallback: AnalyticEventCallback? = null,
    configureCallbackTurbine: Turbine<PaymentOption?>,
    resultCallback: PaymentSheetResultCallback,
) {
    constructor(
        callConfirmOnPaymentOptionCallback: Boolean,
        createIntentCallback: CreateIntentCallback? = null,
        configureCallbackTurbine: Turbine<PaymentOption?>,
        resultCallback: PaymentSheetResultCallback,
    ) : this(
        callConfirmOnPaymentOptionCallback = callConfirmOnPaymentOptionCallback,
        createIntentCallback = createIntentCallback,
        analyticEventCallback = null,
        configureCallbackTurbine = configureCallbackTurbine,
        resultCallback = resultCallback,
    )

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
            createIntentCallback?.let { createIntentCallback(it) }
            analyticEventCallback?.let { analyticEventCallback(it) }
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
