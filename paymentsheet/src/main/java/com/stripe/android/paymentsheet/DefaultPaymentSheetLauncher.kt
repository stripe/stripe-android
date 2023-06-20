package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.DaggerPaymentSheetLauncherComponent
import com.stripe.android.paymentsheet.injection.PaymentSheetLauncherComponent
import com.stripe.android.utils.AnimationConstants
import org.jetbrains.annotations.TestOnly

/**
 * This is used internally for integrations that don't use Jetpack Compose and are
 * able to pass in an activity.
 */
internal class DefaultPaymentSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<PaymentSheetContractV2.Args>,
    private val activity: ComponentActivity,
    lifecycleOwner: LifecycleOwner,
    private val application: Application,
) : PaymentSheetLauncher {
    @InjectorKey
    private val injectorKey: String =
        WeakMapInjectorRegistry.nextKey(requireNotNull(PaymentSheetLauncher::class.simpleName))

    private val paymentSheetLauncherComponent: PaymentSheetLauncherComponent =
        DaggerPaymentSheetLauncherComponent
            .builder()
            .application(application)
            .injectorKey(injectorKey)
            .build()

    init {
        WeakMapInjectorRegistry.register(Injector(paymentSheetLauncherComponent), injectorKey)

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
    )

    override fun present(
        mode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?
    ) {
        val args = PaymentSheetContractV2.Args(
            initializationMode = mode,
            config = configuration,
            injectorKey = injectorKey,
            statusBarColor = activity.window?.statusBarColor,
        )

        val options = ActivityOptionsCompat.makeCustomAnimation(
            application.applicationContext,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        activityResultLauncher.launch(args, options)
    }

    private class Injector(
        private val paymentSheetLauncherComponent: PaymentSheetLauncherComponent,
    ) : NonFallbackInjector {
        override fun inject(injectable: Injectable<*>) {
            when (injectable) {
                is PaymentSheetViewModel.Factory -> {
                    paymentSheetLauncherComponent.inject(injectable)
                }
                is FormViewModel.Factory -> {
                    paymentSheetLauncherComponent.inject(injectable)
                }
                else -> {
                    throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
                }
            }
        }
    }
}
