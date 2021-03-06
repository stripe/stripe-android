package com.stripe.android.paymentsheet.model

import com.stripe.android.PaymentIntentResult
import com.stripe.android.paymentsheet.PaymentOptionResult

internal sealed class ViewState {

    /**
     * The PaymentSheet view state is a little different from the paymentOptions because the
     * PaymentSheet must always go through the processing state.
     * The states always progress as follows: Ready -> StartProcessing -> FinishProcessing -> CloseSheet
     */
    internal sealed class PaymentSheet : ViewState() {
        data class Ready(
            val amount: Long,
            val currencyCode: String
        ) : PaymentSheet()

        object StartProcessing : PaymentSheet()

        data class FinishProcessing(
            val onComplete: () -> Unit
        ) : PaymentSheet()

        data class CloseSheet(
            val result: PaymentIntentResult
        ) : PaymentSheet()
    }

    /**
     * The PaymentOptions may or may not go through a processing state.  The possible flows are:
     * Ready -> CloseSheet, if no save card is required
     * Ready -> StartProcessing -> FinishProcessing -> CloseSheet, if requested to save a new card
     */
    internal sealed class PaymentOptions : ViewState() {
        object Ready : PaymentOptions()

        object StartProcessing : PaymentOptions()

        // Completed indicates the animation should show it completed
        data class FinishProcessing(
            val onComplete: () -> Unit
        ) : PaymentOptions()

        // Finished indicates the activity should be closed immediately
        data class CloseSheet(
            val result: PaymentOptionResult
        ) : PaymentOptions()
    }
}
