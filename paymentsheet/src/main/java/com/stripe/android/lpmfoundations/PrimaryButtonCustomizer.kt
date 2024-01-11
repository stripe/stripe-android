package com.stripe.android.lpmfoundations

import com.stripe.android.core.strings.ResolvableString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Provides the ability to customize the behavior of the primary button (for example the buy button in PaymentSheet).
 */
internal fun interface PrimaryButtonCustomizer {
    /**
     * Return a flow that customizes the [State] of the primary button.
     *
     * Returning `null` will result in no customization, and the behavior implemented by the integration will be used.
     * For example in PaymentSheet we will create the confirm request to confirm the payment intent.
     */
    fun customize(uiState: UiState): Flow<State?>

    /**
     * The customized state of the primary button.
     */
    class State(
        /** The text to be displayed in the primary button. */
        val text: ResolvableString,
        /** Whether the primary button should be enabled. */
        val enabled: Boolean,
        /** The action to take when the customized primary button is clicked */
        val onClick: () -> Unit
    )

    /** Default to no customization. */
    object Default : PrimaryButtonCustomizer {
        override fun customize(uiState: UiState): Flow<State?> {
            return flowOf(null)
        }
    }
}
