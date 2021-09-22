package com.stripe.android.googlepaylauncher.injection

import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherViewModel
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector

/**
 * [Injector] for [GooglePayPaymentMethodLauncherViewModel.Factory].
 */
class GooglePayPaymentMethodLauncherViewModelInjector(
    private val component: GooglePayPaymentMethodLauncherComponent
) : Injector {
    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is GooglePayPaymentMethodLauncherViewModel.Factory -> {
                component.inject(injectable)
            }
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }
}
