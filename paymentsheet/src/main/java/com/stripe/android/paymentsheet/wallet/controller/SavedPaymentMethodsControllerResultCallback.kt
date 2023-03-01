package com.stripe.android.paymentsheet.wallet.controller

import com.stripe.android.paymentsheet.model.PaymentOption
import java.lang.Exception

fun interface SavedPaymentMethodsSheetResultCallback {
    fun onResult(result: SavedPaymentMethodsSheetResult)
}

sealed class SavedPaymentMethodsSheetResult {
    data class Success(
        val paymentOption: PaymentOption?
    ): SavedPaymentMethodsSheetResult()

    data class Error(
        val exception: Exception
    ) : SavedPaymentMethodsSheetResult()

    object Canceled : SavedPaymentMethodsSheetResult()
}