package com.stripe.android.paymentsheet.prototype

import com.stripe.android.model.PaymentMethod

internal interface PaymentMethodDefinition {
    val type: PaymentMethod.Type

    // TODO: Might make sense to extract this.
    fun isSupported(metadata: ParsingMetadata): Boolean {
        val isActivated = !(metadata.stripeIntent.isLiveMode &&
            metadata.stripeIntent.unactivatedPaymentMethods.contains(type.code))
        val meetsRequirements = addRequirements(metadata.hasIntentToSetup()).all { requirement ->
            requirement.meetsRequirements(metadata)
        }
        return isActivated && meetsRequirements
    }

    fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement>

    suspend fun initialAddState(
        metadata: ParsingMetadata,
    ): InitialAddPaymentMethodState

    // TODO: Can we call this somewhere that auto adds the billing info?
    fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams

    // TODO: Should SPM be a separate interface?
    // TODO: Is supported for SPM (pre fetch vs post fetch, for filtering things like cards originating from wallets)
    // TODO: PaymentMethodState for SPM? Or do we just return PM ID and call it good?

    class UiStateKey<V : UiState.Value> private constructor(override val identifier: String) : UiState.Key<V> {
        companion object {
            fun <V : UiState.Value> create(paymentMethodDefinition: PaymentMethodDefinition): UiState.Key<V> {
                return UiStateKey(paymentMethodDefinition.type.code)
            }
        }
    }
}
