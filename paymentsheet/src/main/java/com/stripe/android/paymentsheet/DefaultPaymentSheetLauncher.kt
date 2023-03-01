package com.stripe.android.paymentsheet

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import com.stripe.android.ConfirmCallback
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
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
    private val activityResultLauncher: ActivityResultLauncher<PaymentSheetContractV2.Args>,
    confirmCallback: ConfirmCallback?,
    application: Application
) : PaymentSheetLauncher, NonFallbackInjector {
    @InjectorKey
    private val injectorKey: String =
        WeakMapInjectorRegistry.nextKey(requireNotNull(PaymentSheetLauncher::class.simpleName))

    private val paymentSheetLauncherComponent: PaymentSheetLauncherComponent =
        DaggerPaymentSheetLauncherComponent
            .builder()
            .application(application)
            .confirmCallback(confirmCallback)
            .injectorKey(injectorKey)
            .build()

    init {
        WeakMapInjectorRegistry.register(this, injectorKey)
    }

    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback,
        confirmCallback: ConfirmCallback?
    ) : this(
        activity.registerForActivityResult(
            PaymentSheetContractV2()
        ) {
            callback.onPaymentSheetResult(it)
        },
        confirmCallback,
        activity.application
    )

    constructor(
        fragment: Fragment,
        callback: PaymentSheetResultCallback,
        confirmCallback: ConfirmCallback?
    ) : this(
        fragment.registerForActivityResult(
            PaymentSheetContractV2()
        ) {
            callback.onPaymentSheetResult(it)
        },
        confirmCallback,
        fragment.requireActivity().application
    )

    @TestOnly
    constructor(
        fragment: Fragment,
        registry: ActivityResultRegistry,
        callback: PaymentSheetResultCallback,
        confirmCallback: ConfirmCallback?
    ) : this(
        fragment.registerForActivityResult(
            PaymentSheetContractV2(),
            registry
        ) {
            callback.onPaymentSheetResult(it)
        },
        confirmCallback,
        fragment.requireActivity().application
    )

    override fun present(
        mode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?
    ) {
        val args = PaymentSheetContractV2.Args(
            initializationMode = mode,
            config = configuration,
            injectorKey = injectorKey,
        )
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
