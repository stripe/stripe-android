package com.stripe.android.paymentsheet.model

/**
 * This will show the state of the [PaymentSheetActivity] as it does work.  The states always
 * progress as follows: Ready -> StartProcessing -> FinishProcessing -> ProcessResult
 */
internal sealed class PaymentSheetViewState {
    object Ready : PaymentSheetViewState()

    object StartProcessing : PaymentSheetViewState()

    data class FinishProcessing(
        val onComplete: () -> Unit
    ) : PaymentSheetViewState()
}
