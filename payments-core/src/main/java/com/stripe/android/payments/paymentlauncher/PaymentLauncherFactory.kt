package com.stripe.android.payments.paymentlauncher

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.BuildConfig
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlinx.coroutines.Dispatchers

/**
 * Factory to create a [PaymentLauncher], initialize all required dependencies.
 *
 * Used when [PaymentLauncher] is used as a standalone API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentLauncherFactory(
    private val context: Context,
    private val hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>,
    private val statusBarColor: Int?,
) {

    constructor(
        activity: ComponentActivity,
        callback: PaymentLauncher.PaymentResultCallback
    ) : this(
        context = activity.applicationContext,
        hostActivityLauncher = activity.registerForActivityResult(
            PaymentLauncherContract(),
            callback::onPaymentResult
        ),
        statusBarColor = activity.window?.statusBarColor,
    )

    constructor(
        fragment: Fragment,
        callback: PaymentLauncher.PaymentResultCallback
    ) : this(
        context = fragment.requireActivity().applicationContext,
        hostActivityLauncher = fragment.registerForActivityResult(
            PaymentLauncherContract(),
            callback::onPaymentResult
        ),
        statusBarColor = fragment.requireActivity().window?.statusBarColor,
    )

    fun create(
        publishableKey: String,
        stripeAccountId: String? = null
    ): PaymentLauncher {
        val productUsage = setOf("PaymentLauncher")
        val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
            context,
            { publishableKey },
            productUsage
        )
        return StripePaymentLauncher(
            publishableKeyProvider = { publishableKey },
            stripeAccountIdProvider = { stripeAccountId },
            hostActivityLauncher = hostActivityLauncher,
            statusBarColor = statusBarColor,
            context = context,
            enableLogging = BuildConfig.DEBUG,
            ioContext = Dispatchers.IO,
            uiContext = Dispatchers.Main,
            paymentAnalyticsRequestFactory = analyticsRequestFactory,
            productUsage = productUsage,
        )
    }
}
