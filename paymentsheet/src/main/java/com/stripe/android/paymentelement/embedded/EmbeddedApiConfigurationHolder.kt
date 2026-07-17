package com.stripe.android.paymentelement.embedded

import android.os.Parcelable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

// TODO: Replace with real ApiConfiguration from EmbeddedPaymentElement.Configuration
@Parcelize
internal data class ApiConfiguration(
    val publishableKey: String,
    val stripeAccountId: String? = null,
) : Parcelable

/**
 * Tracks the per-element [ApiConfiguration] so that all network calls use the merchant-supplied
 * credentials when provided, falling back to the global [PaymentConfiguration] singleton when not.
 *
 * Resolution order:
 *   1. [_apiConfiguration].publishableKey / stripeAccountId  (when non-null)
 *   2. [PaymentConfiguration.getInstance] values             (fallback)
 *
 * Initialized from [EmbeddedConfirmationStateHolder] at construction time so it is already
 * populated after process death without any configure() or setState() call.
 */
@Singleton
internal class EmbeddedApiConfigurationHolder @Inject constructor(
    confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val paymentConfig: Provider<PaymentConfiguration>,
) {
    @Volatile
    private var _apiConfiguration: ApiConfiguration? =
        confirmationStateHolder.state?.configuration?.apiConfiguration

    fun update(config: ApiConfiguration?) {
        _apiConfiguration = config
    }

    /** Publishable key: apiConfiguration first, PaymentConfiguration fallback. */
    val publishableKey: String
        get() = _apiConfiguration?.publishableKey ?: paymentConfig.get().publishableKey

    /** Connect account ID: apiConfiguration first, PaymentConfiguration fallback. */
    val stripeAccountId: String?
        get() = _apiConfiguration?.stripeAccountId ?: paymentConfig.get().stripeAccountId
}
