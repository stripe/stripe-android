package com.stripe.android.payments.paymentlauncher

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.BuildConfig
import com.stripe.android.StripeApiBeta
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import kotlinx.coroutines.Dispatchers

/**
 * Factory to create a [PaymentLauncher], initialize all required dependencies.
 *
 * Used when [PaymentLauncher] is used as a standalone API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentLauncherFactory(
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
        stripeAccountId: String? = null,
        betas: Set<StripeApiBeta> = setOf()
    ): PaymentLauncher {
        val productUsage = setOf("PaymentLauncher")
        val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context,
            { publishableKey },
            productUsage
        )
        return StripePaymentLauncher(
            { publishableKey },
            { stripeAccountId },
            hostActivityLauncher,
            context,
            BuildConfig.DEBUG,
            Dispatchers.IO,
            Dispatchers.Main,
            StripeApiRepository(
                context,
                { publishableKey },
                paymentAnalyticsRequestFactory = analyticsRequestFactory,
                betas = betas
            ),
            analyticsRequestFactory,
            productUsage = productUsage
        )
    }
}
