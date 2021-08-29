package com.stripe.android.payments.paymentlauncher

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry

/**
 * Factory to create a [PaymentLauncher].
 */
internal class PaymentLauncherFactory(
    private val context: Context,
    private val hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
) {

    constructor(
        activity: ComponentActivity,
        callback: PaymentLauncher.PaymentResultCallback,
    ) : this(
        activity.applicationContext,
        activity.registerForActivityResult(
            PaymentLauncherContract(),
            callback::onPaymentResult,
        )
    )

    constructor(
        fragment: Fragment,
        callback: PaymentLauncher.PaymentResultCallback
    ) : this(
        fragment.requireActivity().applicationContext,
        fragment.registerForActivityResult(
            PaymentLauncherContract(),
            callback::onPaymentResult
        )
    )

    fun create(
        publishableKey: String,
        stripeAccountId: String? = null
    ): PaymentLauncher {
        val injectorKey = WeakMapInjectorRegistry.nextKey()
        val paymentLauncher =
            StripePaymentLauncher(
                hostActivityLauncher,
                context,
                publishableKey,
                stripeAccountId,
                injectorKey
            )
        WeakMapInjectorRegistry.register(paymentLauncher, injectorKey)
        return paymentLauncher
    }
}
