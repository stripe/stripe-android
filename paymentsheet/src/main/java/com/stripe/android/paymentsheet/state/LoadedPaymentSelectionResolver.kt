package com.stripe.android.paymentsheet.state

import com.stripe.android.paymentsheet.model.PaymentSelection

/**
 * Resolves the final [PaymentSelection] for a freshly loaded [PaymentElementLoader.State].
 *
 * The loader computes an initial selection ([PaymentElementLoader.State.paymentSelection]); this
 * strategy gets the final say, letting an integration preserve the customer's previous selection
 * across reloads/reconfigures when it's still valid. Bound per Dagger graph — see
 * [IdentityLoadedPaymentSelectionResolver] for the default when no integration-specific resolver is
 * provided.
 */
internal fun interface LoadedPaymentSelectionResolver {
    fun resolve(
        state: PaymentElementLoader.State,
        integrationConfiguration: PaymentElementLoader.Configuration,
        reconfigureContext: PaymentElementLoader.ReconfigureContext?,
    ): PaymentSelection?
}

/**
 * Default resolver: keeps the loader's freshly computed selection. Used by integrations that don't
 * preserve a previous selection (e.g. the PaymentSheet launcher and Link/crypto onramp).
 */
internal object IdentityLoadedPaymentSelectionResolver : LoadedPaymentSelectionResolver {
    override fun resolve(
        state: PaymentElementLoader.State,
        integrationConfiguration: PaymentElementLoader.Configuration,
        reconfigureContext: PaymentElementLoader.ReconfigureContext?,
    ): PaymentSelection? = state.paymentSelection
}
