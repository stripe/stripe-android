package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.rememberPaymentSheetFlowController

internal class FlowControllerTestFactory(
    private val integrationType: IntegrationType,
    private val createIntentCallback: CreateIntentCallback? = null,
    private val paymentOptionCallback: PaymentOptionCallback,
    private val resultCallback: PaymentSheetResultCallback,
) {

    fun make(activity: ComponentActivity): PaymentSheet.FlowController {
        return when (integrationType) {
            IntegrationType.Activity -> forActivity(activity)
            IntegrationType.Compose -> forCompose(activity)
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

    private fun forCompose(activity: ComponentActivity): PaymentSheet.FlowController {
        lateinit var flowController: PaymentSheet.FlowController
        activity.setContent {
            flowController = if (createIntentCallback != null) {
                rememberPaymentSheetFlowController(
                    createIntentCallback = createIntentCallback,
                    paymentOptionCallback = { paymentOption ->
                        paymentOptionCallback.onPaymentOption(paymentOption)
                        flowController.confirm()
                    },
                    paymentResultCallback = resultCallback,
                )
            } else {
                rememberPaymentSheetFlowController(
                    paymentOptionCallback = { paymentOption ->
                        paymentOptionCallback.onPaymentOption(paymentOption)
                        flowController.confirm()
                    },
                    paymentResultCallback = resultCallback,
                )
            }
        }
        return flowController
    }
}
