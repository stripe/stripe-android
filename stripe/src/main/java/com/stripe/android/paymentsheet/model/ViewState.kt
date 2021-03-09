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

        data class FinishProcessing(
            val result: PaymentIntentResult
        ) : PaymentSheet() {
            var onComplete: () -> Unit = {}

            internal companion object {
                fun create(
                    result: PaymentIntentResult,
                    onComplete: () -> Unit
                ): FinishProcessing {
                    return FinishProcessing(
                        result
                    ).also {
                        it.onComplete = onComplete
                    }
                }
            }
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

        data class FinishProcessing(
            val result: PaymentOptionResult
        ) : PaymentOptions() {
            var onComplete: () -> Unit = {}

            internal companion object {
                fun create(
                    result: PaymentOptionResult,
                    onComplete: () -> Unit
                ): FinishProcessing {
                    return FinishProcessing(
                        result
                    ).also {
                        it.onComplete = onComplete
                    }
                }
            }
        }

        data class ProcessResult(
            val result: PaymentOptionResult
        ) : PaymentOptions()
    }
}
