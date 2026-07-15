package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.ApiConfiguration
import com.stripe.android.PaymentConfiguration
import javax.inject.Inject

/**
 * Mutable holder that provides the [ApiConfiguration.State] for a single Dagger component scope.
 *
 * When [apiConfiguration] is null (the default), [get] falls back to
 * [PaymentConfiguration.getInstance] converted to an [ApiConfiguration.State] — the same global
 * singleton the SDK has always used. Setting a non-null value overrides the singleton for all
 * network requests bound to this component (i.e., this [EmbeddedPaymentElement] instance).
 */
class ApiConfigurationHolder @Inject constructor(
    private val context: Context,
) {
    @Volatile
    var apiConfiguration: ApiConfiguration.State? = null

    fun get(): PaymentConfiguration =
        apiConfiguration?.toPaymentConfiguration() ?: PaymentConfiguration.getInstance(context)
}
