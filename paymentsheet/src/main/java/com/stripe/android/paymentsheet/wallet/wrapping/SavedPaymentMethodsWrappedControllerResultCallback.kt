package com.stripe.android.paymentsheet.wallet.wrapping

import com.stripe.android.paymentsheet.model.PaymentOption
import java.lang.Exception

fun interface SavedPaymentMethodsControllerResultCallback {
    fun onResult(result: SavedPaymentMethodsControllerResult)
}

sealed class SavedPaymentMethodsControllerResult {
    data class Success(
        val paymentOption: PaymentOption?
    ): SavedPaymentMethodsControllerResult()

    data class Error(
        val exception: Exception
    ) : SavedPaymentMethodsControllerResult()

    object Canceled : SavedPaymentMethodsControllerResult()
}