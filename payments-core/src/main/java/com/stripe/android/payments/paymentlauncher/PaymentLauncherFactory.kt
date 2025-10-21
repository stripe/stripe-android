package com.stripe.android.payments.paymentlauncher

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.BuildConfig
import com.stripe.android.core.reactnative.ReactNativeSdkInternal
import com.stripe.android.core.reactnative.UnregisterSignal
import com.stripe.android.core.reactnative.registerForReactNativeActivityResult
import com.stripe.android.core.utils.StatusBarCompat

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
        statusBarColor = StatusBarCompat.color(activity),
    )

    @ReactNativeSdkInternal
    constructor(
        activity: ComponentActivity,
        signal: UnregisterSignal,
        callback: PaymentLauncher.InternalPaymentResultCallback
    ) : this(
        hostActivityLauncher = registerForReactNativeActivityResult(
            activity,
            signal,
            PaymentLauncherContract(),
            callback::onPaymentResult
        ),
        statusBarColor = StatusBarCompat.color(activity),
    )

    constructor(
        fragment: Fragment,
        callback: PaymentLauncher.InternalPaymentResultCallback
    ) : this(
        hostActivityLauncher = fragment.registerForActivityResult(
            PaymentLauncherContract(),
            callback::onPaymentResult
        ),
        statusBarColor = StatusBarCompat.color(fragment.requireActivity()),
    )

    constructor(
        activityResultRegistryOwner: ActivityResultRegistryOwner,
        lifecycleOwner: LifecycleOwner,
        statusBarColor: Int?,
        callback: PaymentLauncher.InternalPaymentResultCallback
    ) : this(
        hostActivityLauncher = activityResultRegistryOwner.activityResultRegistry.register(
            "PaymentLauncherFactory_hostActivityLauncher",
            PaymentLauncherContract(),
            callback::onPaymentResult
        ),
        statusBarColor = statusBarColor
    ) {
        check(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED))

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    hostActivityLauncher.unregister()
                }
            }
        )
    }

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
            includePaymentSheetNextHandlers = false,
        )
    }
}
