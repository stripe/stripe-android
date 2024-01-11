package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.lpmfoundations.AddPaymentMethodUiDefinition
import com.stripe.android.lpmfoundations.InitialAddPaymentMethodState
import com.stripe.android.lpmfoundations.PrimaryButtonCustomizer
import com.stripe.android.lpmfoundations.UiState

internal suspend fun PaymentMethodDefinition.buildInitialState(
    metadata: PaymentMethodMetadata,
    builder: suspend InitialAddPaymentMethodStateBuilder.() -> Unit,
): InitialAddPaymentMethodState {
    return InitialAddPaymentMethodStateBuilder(this, metadata).apply {
        builder()
    }.build()
}

internal class InitialAddPaymentMethodStateBuilder(
    private val paymentMethodDefinition: PaymentMethodDefinition,
    private val metadata: PaymentMethodMetadata,
) {
    private var state: UiState.Value? = null
    private lateinit var addPaymentMethodUiDefinition: AddPaymentMethodUiDefinition
    private var primaryButtonCustomizer: PrimaryButtonCustomizer = PrimaryButtonCustomizer.Default

    fun state(state: UiState.Value) {
        this.state = state
    }

    suspend fun uiDefinition(builder: suspend AddPaymentMethodUiDefinitionBuilder.() -> Unit) {
        this.addPaymentMethodUiDefinition = AddPaymentMethodUiDefinitionBuilder(paymentMethodDefinition, metadata)
            .apply {
                builder()
            }.build()
    }

    fun primaryButtonCustomizer(primaryButtonCustomizer: PrimaryButtonCustomizer) {
        this.primaryButtonCustomizer = primaryButtonCustomizer
    }

    fun build(): InitialAddPaymentMethodState {
        return InitialAddPaymentMethodState(
            state = state,
            addPaymentMethodUiDefinition = addPaymentMethodUiDefinition,
            primaryButtonCustomizer = primaryButtonCustomizer
        ) { uiState ->
            // TODO(jaynewstrom): Add billing details
            paymentMethodDefinition.addConfirmParams(uiState)
        }
    }
}
