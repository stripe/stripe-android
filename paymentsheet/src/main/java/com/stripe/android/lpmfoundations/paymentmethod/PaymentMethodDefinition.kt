package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.lpmfoundations.InitialAddPaymentMethodState
import com.stripe.android.lpmfoundations.PaymentMethodConfirmParams
import com.stripe.android.lpmfoundations.UiState
import com.stripe.android.model.PaymentMethod

internal interface PaymentMethodDefinition {
    /**
     * The payment method type, for example: PaymentMethod.Type.Card, etc.
     */
    val type: PaymentMethod.Type

    /**
     * The requirements that need to be met in order for this Payment Method to be available for selection by the buyer.
     * For example emptySet() if no requirements exist (this payment method is always supported if it exists in ``.
     * Or setOf(AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods) if the payment method requires the
     * merchant to provide a PaymentSheet.Configuration with delayed payment methods enabled.
     */
    fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement>

    /**
     * Creates the [InitialAddPaymentMethodState] used to define how to handle a payment method.
     */
    suspend fun initialAddState(
        metadata: PaymentMethodMetadata,
    ): InitialAddPaymentMethodState

    /**
     * The confirm params for the payment method.
     */
    fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams

    /**
     * [UiState.Key] associated with the given [PaymentMethodDefinition].
     *
     * Uses the [PaymentMethodDefinition.type] for the [UiState.Key.identifier].
     */
    class UiStateKey<V : UiState.Value> private constructor(override val identifier: String) : UiState.Key<V> {
        companion object {
            fun <V : UiState.Value> create(paymentMethodDefinition: PaymentMethodDefinition): UiState.Key<V> {
                return UiStateKey(paymentMethodDefinition.type.code)
            }
        }
    }
}
