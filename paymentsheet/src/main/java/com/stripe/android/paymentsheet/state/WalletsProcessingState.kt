package com.stripe.android.paymentsheet.state

import com.stripe.android.core.strings.ResolvableString

internal sealed interface WalletsProcessingState {
    data class Idle(val error: ResolvableString?) : WalletsProcessingState

    data object Processing : WalletsProcessingState

    class Completed(val onComplete: () -> Unit) : WalletsProcessingState
}
