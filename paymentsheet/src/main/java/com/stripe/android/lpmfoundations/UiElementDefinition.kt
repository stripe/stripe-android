package com.stripe.android.lpmfoundations

/**
 * Defines a UI element. When it's ready to be displayed, the [renderer] method is called with the current [UiState].
 */
internal interface UiElementDefinition {
    /**
     * Returns the [UiRenderer] given the [UiState].
     */
    fun renderer(uiState: UiState): UiRenderer
}
