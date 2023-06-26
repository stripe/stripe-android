package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
internal class PaymentSheetTestFactory(
    private val integrationType: IntegrationType,
    private val createIntentCallback: CreateIntentCallback? = null,
    private val resultCallback: PaymentSheetResultCallback,
) {

    enum class IntegrationType {
        Activity,
    }

    fun make(activity: ComponentActivity): PaymentSheet {
        return when (integrationType) {
            IntegrationType.Activity -> forActivity(activity)
        }
    }

    private fun forActivity(activity: ComponentActivity): PaymentSheet {
        return if (createIntentCallback != null) {
            PaymentSheet(activity, createIntentCallback, resultCallback)
        } else {
            PaymentSheet(activity, resultCallback)
        }
    }
}
