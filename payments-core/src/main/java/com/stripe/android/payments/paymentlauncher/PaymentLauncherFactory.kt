package com.stripe.android.payments.paymentlauncher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import com.stripe.android.BuildConfig

/**
 * Factory to create a [PaymentLauncher], initialize all required dependencies.
 *
 * Used when [PaymentLauncher] is used as a standalone API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentLauncherFactory(
    private val hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>,
    private val statusBarColor: Int?,
) {

    constructor(
        activity: ComponentActivity,
        callback: PaymentLauncher.InternalPaymentResultCallback
    ) : this(
        hostActivityLauncher = activity.registerForActivityResult(
            PaymentLauncherContract(),
            callback::onPaymentResult
        ),
        statusBarColor = activity.window?.statusBarColor,
    )

    constructor(
        fragment: Fragment,
        callback: PaymentLauncher.InternalPaymentResultCallback
    ) : this(
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
        return StripePaymentLauncher(
            publishableKeyProvider = { publishableKey },
            stripeAccountIdProvider = { stripeAccountId },
            hostActivityLauncher = hostActivityLauncher,
            statusBarColor = statusBarColor,
            enableLogging = BuildConfig.DEBUG,
            productUsage = productUsage,
            includePaymentSheetAuthenticators = false,
        )
    }
}
