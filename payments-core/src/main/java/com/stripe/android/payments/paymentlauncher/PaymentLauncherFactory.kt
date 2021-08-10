package com.stripe.android.payments.paymentlauncher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment

/**
 * Factory to create a [PaymentLauncher].
 */
internal class PaymentLauncherFactory(
    private val hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
) {

    constructor(
        activity: ComponentActivity,
        callback: PaymentLauncher.PaymentResultCallback,
    ) : this(
        activity.registerForActivityResult(
            PaymentLauncherContract(),
            callback::onPaymentResult,
        )
    )

    constructor(
        fragment: Fragment,
        callback: PaymentLauncher.PaymentResultCallback
    ) : this(
        fragment.registerForActivityResult(
            PaymentLauncherContract(),
            callback::onPaymentResult
        )
    )

    fun create(
        publishableKey: String,
        stripeAccountId: String? = null
    ): PaymentLauncher = StripePaymentLauncher(hostActivityLauncher, publishableKey, stripeAccountId)
}
