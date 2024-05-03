package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.rememberPaymentSheet

internal class PaymentSheetTestFactory(
    private val integrationType: IntegrationType,
    private val createIntentCallback: CreateIntentCallback? = null,
    private val resultCallback: PaymentSheetResultCallback,
) {

    fun make(activity: ComponentActivity): PaymentSheet {
        return when (integrationType) {
            IntegrationType.Activity -> forActivity(activity)
            IntegrationType.Compose -> forCompose(activity)
        }
    }

    private fun forActivity(activity: ComponentActivity): PaymentSheet {
        return if (createIntentCallback != null) {
            PaymentSheet(activity, createIntentCallback, resultCallback)
        } else {
            PaymentSheet(activity, resultCallback)
        }
    }

    private fun forCompose(activity: ComponentActivity): PaymentSheet {
        lateinit var paymentSheet: PaymentSheet
        activity.setContent {
            paymentSheet = if (createIntentCallback != null) {
                rememberPaymentSheet(createIntentCallback, resultCallback)
            } else {
                rememberPaymentSheet(resultCallback)
            }
        }
        return paymentSheet
    }
}
