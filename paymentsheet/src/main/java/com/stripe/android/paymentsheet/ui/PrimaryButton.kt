package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.ResolvableString

internal object PrimaryButton {
    internal sealed class State(
        val isProcessing: Boolean
    ) {
        data object Ready : State(
            isProcessing = false
        )

        data object StartProcessing : State(
            isProcessing = true
        )

        data class FinishProcessing(
            val onComplete: () -> Unit
        ) : State(
            isProcessing = true
        )
    }

    internal data class UIState(
        val label: ResolvableString,
        val canClickWhileDisabled: Boolean,
        val onClick: () -> Unit,
        val onDisabledClick: () -> Unit,
        val enabled: Boolean,
        val lockVisible: Boolean,
    )
}
