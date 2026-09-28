package com.stripe.android.paymentelement.taptoadd

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheetResult

fun interface TapToAddResultTestCallback {
    fun onResult(result: TapToAddTestResult)
}

sealed interface TapToAddTestResult {
    data object Completed : TapToAddTestResult

    data class Failed(val error: Throwable) : TapToAddTestResult

    companion object {
        fun from(result: PaymentSheetResult): TapToAddTestResult {
            return when (result) {
                is PaymentSheetResult.Completed -> Completed
                is PaymentSheetResult.Failed -> Failed(result.error)
                is PaymentSheetResult.Canceled -> throw IllegalArgumentException("Cancel result unexpected!")
            }
        }

        fun from(result: EmbeddedPaymentElement.Result): TapToAddTestResult {
            return when (result) {
                is EmbeddedPaymentElement.Result.Completed -> Completed
                is EmbeddedPaymentElement.Result.Failed -> Failed(result.error)
                is EmbeddedPaymentElement.Result.Canceled ->
                    throw IllegalArgumentException("Cancel result unexpected!")
            }
        }
    }
}
