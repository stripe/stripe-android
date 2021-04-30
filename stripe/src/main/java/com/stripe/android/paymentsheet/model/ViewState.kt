package com.stripe.android.paymentsheet.model

import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal sealed class ViewState {
    /**
     * The PaymentSheet view state is a little different from the PaymentOptions because the
     * PaymentSheet must always go through the processing state.
     * The states always progress as follows: Ready -> StartProcessing -> FinishProcessing -> ProcessResult
     */
    internal sealed class PaymentSheet(private val userErrorMessage: BaseSheetViewModel.UserErrorMessage) : ViewState() {
        data class Ready(val message: BaseSheetViewModel.UserErrorMessage) : PaymentSheet(message)

        data class StartProcessing(val message: BaseSheetViewModel.UserErrorMessage) : PaymentSheet(message)

        data class FinishProcessing(
            val message: BaseSheetViewModel.UserErrorMessage,
            val onComplete: () -> Unit
        ) : PaymentSheet(message)
    }

    /**
     * The PaymentOptions may or may not go through a processing state. The possible state transitions are:
     * Ready -> ProcessResult, if no save card is required
     * Ready -> StartProcessing -> FinishProcessing -> ProcessResult, if requested to save a new card
     */
    internal sealed class PaymentOptions : ViewState() {
        object Ready : PaymentOptions()

        object StartProcessing : PaymentOptions()

        data class FinishProcessing(
            val onComplete: () -> Unit
        ) : PaymentOptions()
    }
}
