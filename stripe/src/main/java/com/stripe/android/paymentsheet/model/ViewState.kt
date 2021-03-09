package com.stripe.android.paymentsheet.model

import com.stripe.android.PaymentIntentResult
import com.stripe.android.paymentsheet.PaymentOptionResult

internal sealed class ViewState {
    /**
     * The PaymentSheet view state is a little different from the PaymentOptions because the
     * PaymentSheet must always go through the processing state.
     * The states always progress as follows: Ready -> StartProcessing -> FinishProcessing -> ProcessResult
     */
    internal sealed class PaymentSheet : ViewState() {
        data class Ready(
            val amount: Long,
            val currencyCode: String
        ) : PaymentSheet()

        object StartProcessing : PaymentSheet()

        class FinishProcessing(
            val onComplete: () -> Unit
        ) : PaymentSheet() {
            /**
             * Do not compare [onComplete] as part of [equals].
             */
            override fun equals(other: Any?): Boolean = other is FinishProcessing

            override fun hashCode(): Int = onComplete.hashCode()
        }

        data class ProcessResult(
            val result: PaymentIntentResult
        ) : PaymentSheet()
    }

    /**
     * The PaymentOptions may or may not go through a processing state. The possible state transitions are:
     * Ready -> ProcessResult, if no save card is required
     * Ready -> StartProcessing -> FinishProcessing -> ProcessResult, if requested to save a new card
     */
    internal sealed class PaymentOptions : ViewState() {
        object Ready : PaymentOptions()

        object StartProcessing : PaymentOptions()

        class FinishProcessing(
            val onComplete: () -> Unit
        ) : PaymentOptions() {
            /**
             * Do not compare [onComplete] as part of [equals].
             */
            override fun equals(other: Any?): Boolean = other is FinishProcessing

            override fun hashCode(): Int = onComplete.hashCode()
        }

        data class ProcessResult(
            val result: PaymentOptionResult
        ) : PaymentOptions()
    }
}
