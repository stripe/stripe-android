package com.stripe.android.paymentsheet.model

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.PaymentSheetActivity

/**
 * This will show the state of the [PaymentSheetActivity] as it does work.  The states always
 * progress as follows: Ready -> StartProcessing -> FinishProcessing -> ProcessResult
 */
internal sealed class PaymentSheetViewState(
    val errorMessage: UserErrorMessage? = null
) {
    data class Reset(private val message: UserErrorMessage? = null) :
        PaymentSheetViewState(message)

    data object StartProcessing : PaymentSheetViewState(null)

    data class FinishProcessing(
        val onComplete: () -> Unit
    ) : PaymentSheetViewState()

    data class UserErrorMessage(val message: ResolvableString)
}
