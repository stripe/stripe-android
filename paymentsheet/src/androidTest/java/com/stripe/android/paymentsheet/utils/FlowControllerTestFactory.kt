package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
internal class FlowControllerTestFactory(
    private val integrationType: IntegrationType,
    private val createIntentCallback: CreateIntentCallback? = null,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val resultCallback: PaymentSheetResultCallback,
) {

    enum class IntegrationType {
        Activity,
    }

    fun make(activity: ComponentActivity): PaymentSheet.FlowController {
        return when (integrationType) {
            IntegrationType.Activity -> forActivity(activity)
        }
    }

    private fun forActivity(activity: ComponentActivity): PaymentSheet.FlowController {
        lateinit var flowController: PaymentSheet.FlowController
        flowController = if (createIntentCallback != null) {
            PaymentSheet.FlowController.create(
                activity = activity,
                paymentOptionCallback = { paymentOption ->
                    paymentOptionCallback.onPaymentOption(paymentOption)
                    flowController.confirm()
                },
                createIntentCallback = createIntentCallback,
                paymentResultCallback = resultCallback,
            )
        } else {
            PaymentSheet.FlowController.create(
                activity = activity,
                paymentOptionCallback = { paymentOption ->
                    paymentOptionCallback.onPaymentOption(paymentOption)
                    flowController.confirm()
                },
                paymentResultCallback = resultCallback,
            )
        }
        return flowController
    }
}
