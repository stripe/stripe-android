package com.stripe.android.lpmfoundations

/**
 * Defines a UI element. When it's ready to be displayed, the [renderer] method is called with the current [UiState].
 */
internal interface UiElementDefinition {
    /**
     * Returns true if the given state is valid.
     *
     * When all [UiElementDefinition] in a [AddPaymentMethodUiDefinition] return true, the primary button will be
     *  enabled, and the buyer can complete their purchase.
     *
     * An example of an invalid state (returning false) could be when the buyer hasn't filled out a required text field.
     */
    fun isValid(uiState: UiState.Snapshot): Boolean

    /**
     * Returns the [UiRenderer] given the [UiState].
     */
    fun renderer(uiState: UiState): UiRenderer
}
