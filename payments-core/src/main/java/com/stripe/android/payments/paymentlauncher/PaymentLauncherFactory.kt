package com.stripe.android.payments.paymentlauncher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment

/**
 * Factory to create a [PaymentLauncher].
 */
internal class PaymentLauncherFactory(
    private val hostActivityLauncher: ActivityResultLauncher<PaymentLauncherHostContract.Args>
) {

    constructor(
        activity: ComponentActivity,
        callback: PaymentLauncher.PaymentResultCallback
    ) : this(
        activity.registerForActivityResult(
            PaymentLauncherHostContract(),
            callback::onPaymentResult
        )
    )

    constructor(
        fragment: Fragment,
        callback: PaymentLauncher.PaymentResultCallback
    ) : this(
        fragment.registerForActivityResult(
            PaymentLauncherHostContract(),
            callback::onPaymentResult
        )
    )

    fun create(): PaymentLauncher = StripePaymentLauncher(hostActivityLauncher)
}
