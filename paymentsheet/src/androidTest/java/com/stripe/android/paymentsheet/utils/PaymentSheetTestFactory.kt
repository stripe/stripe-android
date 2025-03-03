package com.stripe.android.paymentsheet.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.rememberPaymentSheet

internal class PaymentSheetTestFactory(
    private val createIntentCallback: CreateIntentCallback? = null,
    private val resultCallback: PaymentSheetResultCallback,
) {

    fun make(activity: ComponentActivity): PaymentSheet {
        return if (createIntentCallback != null) {
            PaymentSheet(activity, createIntentCallback, resultCallback)
        } else {
            PaymentSheet(activity, resultCallback)
        }
    }

    @Composable
    fun make(): PaymentSheet {
        return if (createIntentCallback != null) {
            rememberPaymentSheet(createIntentCallback, resultCallback)
        } else {
            rememberPaymentSheet(resultCallback)
        }
    }
}
