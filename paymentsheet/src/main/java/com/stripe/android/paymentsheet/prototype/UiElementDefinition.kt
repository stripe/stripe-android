package com.stripe.android.paymentsheet.prototype

internal interface UiElementDefinition {
    // TODO: Should this be a param on the UiState.Value instead?
    fun isComplete(uiState: UiState.Snapshot): Boolean

    fun renderer(uiState: UiState): UiRenderer
}
