package com.stripe.android.paymentsheet.prototype

import com.stripe.android.paymentsheet.prototype.uielement.DropdownItem
import com.stripe.android.ui.core.elements.DropdownSpec

internal class InitialAddPaymentMethodState(
    val state: UiState.Value?,
    val addPaymentMethodUiDefinition: AddPaymentMethodUiDefinition,
    val primaryButtonCustomizer: PrimaryButtonCustomizer,
    val confirmClickHandler: (UiState.Snapshot) -> PaymentMethodConfirmParams,
)

internal suspend fun PaymentMethodDefinition.buildInitialState(
    metadata: ParsingMetadata,
    builder: suspend InitialAddPaymentMethodStateBuilder.() -> Unit,
): InitialAddPaymentMethodState {
    return InitialAddPaymentMethodStateBuilder(this, metadata).apply {
        builder()
    }.build()
}

internal class InitialAddPaymentMethodStateBuilder(
    private val paymentMethodDefinition: PaymentMethodDefinition,
    private val metadata: ParsingMetadata,
) {
    // TODO: should this be a list instead?
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

    fun parseDropdownItems(v1ApiPath: String): List<DropdownItem>? {
        val spec = metadata.sharedDataSpecs.firstOrNull { it.type == paymentMethodDefinition.type.code }
        val dropdownSpec = spec?.fields?.firstOrNull { it.apiPath.v1 == v1ApiPath } as? DropdownSpec?
        return dropdownSpec?.items?.map { DropdownItem(it.apiValue.orEmpty(), it.displayText) }
    }

    fun build(): InitialAddPaymentMethodState {
        return InitialAddPaymentMethodState(
            state = state,
            addPaymentMethodUiDefinition = addPaymentMethodUiDefinition,
            primaryButtonCustomizer = primaryButtonCustomizer
        ) { uiState ->
            // TODO: Add billing details
            paymentMethodDefinition.addConfirmParams(uiState)
        }
    }
}
