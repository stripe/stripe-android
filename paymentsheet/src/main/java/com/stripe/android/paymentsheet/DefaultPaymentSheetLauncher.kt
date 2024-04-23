package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.utils.AnimationConstants
import org.jetbrains.annotations.TestOnly

/**
 * This is used internally for integrations that don't use Jetpack Compose and are
 * able to pass in an activity.
 */
internal class DefaultPaymentSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<PaymentSheetContractV2.Args>,
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val application: Application,
    private val callback: PaymentSheetResultCallback,
) : PaymentSheetLauncher {
    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    IntentConfirmationInterceptor.createIntentCallback = null
                    super.onDestroy(owner)
                }
            }
        )
    }

    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        activityResultLauncher = activity.registerForActivityResult(
            PaymentSheetContractV2()
        ) {
            callback.onPaymentSheetResult(it)
        },
        activity = activity,
        lifecycleOwner = activity,
        application = activity.application,
        callback = callback,
    )

    constructor(
        fragment: Fragment,
        callback: PaymentSheetResultCallback
    ) : this(
        activityResultLauncher = fragment.registerForActivityResult(
            PaymentSheetContractV2()
        ) {
            callback.onPaymentSheetResult(it)
        },
        activity = fragment.requireActivity(),
        lifecycleOwner = fragment,
        application = fragment.requireActivity().application,
        callback = callback,
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: PaymentSheetResultCallback
    ) : this(
        activityResultLauncher = fragment.registerForActivityResult(
            PaymentSheetContractV2(),
            registry
        ) {
            callback.onPaymentSheetResult(it)
        },
        activity = fragment.requireActivity(),
        lifecycleOwner = fragment,
        application = fragment.requireActivity().application,
        callback = callback,
    )

    override fun present(
        mode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?
    ) {
        val args = PaymentSheetContractV2.Args(
            initializationMode = mode,
            config = configuration ?: PaymentSheet.Configuration.default(activity),
            statusBarColor = activity.window?.statusBarColor,
        )

        val options = ActivityOptionsCompat.makeCustomAnimation(
            application.applicationContext,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        try {
            activityResultLauncher.launch(args, options)
        } catch (e: IllegalStateException) {
            val message = "The host activity is not in a valid state (${lifecycleOwner.lifecycle.currentState})."
            callback.onPaymentSheetResult(PaymentSheetResult.Failed(IllegalStateException(message, e)))
        }
    }
}
