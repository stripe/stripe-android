package com.stripe.android.paymentsheet.model

import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

/**
 * This will show the state of the [PaymentSheetActivity] as it does work.  The states always
 * progress as follows: Ready -> StartProcessing -> FinishProcessing -> ProcessResult
 */
internal sealed class PaymentSheetViewState(
    val errorMessage: BaseSheetViewModel.UserErrorMessage? = null
) {
    data class Reset(private val message: BaseSheetViewModel.UserErrorMessage? = null) :
        PaymentSheetViewState(message)

    object StartProcessing : PaymentSheetViewState(null)

    data class FinishProcessing(
        val onComplete: () -> Unit
    ) : PaymentSheetViewState()
}
