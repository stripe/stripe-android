package com.stripe.android.paymentsheet.prototype

import com.stripe.android.core.strings.ResolvableString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal interface PrimaryButtonCustomizer {
    fun customize(uiState: UiState): Flow<State?>

    class State(val text: ResolvableString, val enabled: Boolean, val onClick: () -> Unit)

    /** Default to no customization. */
    object Default : PrimaryButtonCustomizer {
        override fun customize(uiState: UiState): Flow<State?> {
            return flowOf(null)
        }
    }
}
