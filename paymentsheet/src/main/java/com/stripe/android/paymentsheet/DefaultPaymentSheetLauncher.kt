package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.DaggerPaymentSheetLauncherComponent
import com.stripe.android.paymentsheet.injection.PaymentSheetLauncherComponent
import org.jetbrains.annotations.TestOnly

/**
 * This is used internally for integrations that don't use Jetpack Compose and are
 * able to pass in an activity.
 */
internal class DefaultPaymentSheetLauncher(
    private val activityResultLauncher: ActivityResultLauncher<PaymentSheetContract.Args>,
    application: Application
) : PaymentSheetLauncher, Injector {
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
        WeakMapInjectorRegistry.register(this, injectorKey)
    }

    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        activity.registerForActivityResult(
            PaymentSheetContract()
        ) {
            callback.onPaymentSheetResult(it)
        },
        activity.application
    )

    constructor(
        fragment: Fragment,
        callback: PaymentSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            PaymentSheetContract()
        ) {
            callback.onPaymentSheetResult(it)
        },
        fragment.requireActivity().application
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: PaymentSheetResultCallback
    ) : this(
        fragment.registerForActivityResult(
            PaymentSheetContract(),
            registry
        ) {
            callback.onPaymentSheetResult(it)
        },
        fragment.requireActivity().application
    )

    override fun presentWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?
    ) = present(
        PaymentSheetContract.Args.createPaymentIntentArgsWithInjectorKey(
            paymentIntentClientSecret,
            configuration,
            injectorKey
        )
    )

    override fun presentWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: PaymentSheet.Configuration?
    ) = present(
        PaymentSheetContract.Args.createSetupIntentArgsWithInjectorKey(
            setupIntentClientSecret,
            configuration,
            injectorKey
        )
    )

    private fun present(args: PaymentSheetContract.Args) {
        activityResultLauncher.launch(args)
    }

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
