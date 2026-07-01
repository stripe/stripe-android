package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.PaymentConfiguration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mutable holder that provides the [PaymentConfiguration] for a single Dagger component scope.
 *
 * When [paymentConfiguration] is null (the default), [get] falls back to
 * [PaymentConfiguration.getInstance] — the same global singleton the SDK has always used.
 * Setting a non-null value overrides the singleton for all network requests bound to this
 * component (i.e., this [EmbeddedPaymentElement] instance).
 */
@Singleton
class PaymentConfigurationHolder @Inject constructor(
    private val context: Context,
) {
    @Volatile
    var paymentConfiguration: PaymentConfiguration? = null

    fun get(): PaymentConfiguration =
        paymentConfiguration ?: PaymentConfiguration.getInstance(context)
}
