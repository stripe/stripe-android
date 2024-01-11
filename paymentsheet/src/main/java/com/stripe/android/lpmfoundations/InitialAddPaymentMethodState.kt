package com.stripe.android.lpmfoundations

/**
 * The entry point/wrapper for how to create [UiState] and what will be used by the integrators
 *  (PaymentSheet/FlowController/CustomerSheet) to process adding a new payment method.
 */
internal class InitialAddPaymentMethodState(
    /**
     * The initial state of the payment method.
     *
     * Note: This may or may not be used when it comes time to create [UiState] due to how [UiState] can be restored.
     */
    val state: UiState.Value?,

    /**
     * The UI details for displaying the payment method information to the buyer.
     */
    val addPaymentMethodUiDefinition: AddPaymentMethodUiDefinition,

    /**
     * The customization of the primary button.
     */
    val primaryButtonCustomizer: PrimaryButtonCustomizer,

    /**
     * The action that creates the [PaymentMethodConfirmParams] for the payment method.
     */
    val confirmClickHandler: (UiState.Snapshot) -> PaymentMethodConfirmParams,
)
