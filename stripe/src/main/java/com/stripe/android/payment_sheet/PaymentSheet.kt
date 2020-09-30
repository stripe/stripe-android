package com.stripe.android.payment_sheet

import androidx.activity.ComponentActivity

internal class PaymentSheet(val clientSecret: String, val ephemeralKey: String, val customerId: String) {
    fun confirm(activity: ComponentActivity, callback: (CompletionStatus) -> Unit) {
        // TODO: Actually handle result
        PaymentSheetActivityStarter(activity)
            .startForResult(PaymentSheetActivityStarter.Args(clientSecret, ephemeralKey, customerId))
    }

    internal sealed class CompletionStatus {
        object Succeeded : CompletionStatus()
        data class Failed(val error: Throwable) : CompletionStatus()
        object Cancelled : CompletionStatus()
    }
}
