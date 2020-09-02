package com.stripe.android

import androidx.activity.ComponentActivity

internal class Checkout(val clientSecret: String, val ephemeralKey: String, val customerId: String) {
    fun confirm(activity: ComponentActivity, callback: (CompletionStatus) -> Unit) {
        // TODO: Actually handle result
        CheckoutActivityStarter(activity)
            .startForResult(CheckoutActivityStarter.Args(clientSecret, ephemeralKey, customerId))
    }

    internal sealed class CompletionStatus {
        object Succeeded : CompletionStatus()
        data class Failed(val error: Throwable) : CompletionStatus()
        object Cancelled : CompletionStatus()
    }
}
