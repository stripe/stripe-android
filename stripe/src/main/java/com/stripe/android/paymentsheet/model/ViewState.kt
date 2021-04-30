package com.stripe.android.paymentsheet.model

internal sealed class ViewState {
    /**
     * The PaymentSheet view state is a little different from the PaymentOptions because the
     * PaymentSheet must always go through the processing state.
     * The states always progress as follows: Ready -> StartProcessing -> FinishProcessing -> ProcessResult
     */
    internal sealed class PaymentSheet : ViewState() {
        object Ready : PaymentSheet()

        object StartProcessing : PaymentSheet()

        data class FinishProcessing(
            val onComplete: () -> Unit
        ) : PaymentSheet()
    }

    /**
     * The PaymentOptions does not do any processing
     */
    object PaymentOptionsReady : ViewState()
}
