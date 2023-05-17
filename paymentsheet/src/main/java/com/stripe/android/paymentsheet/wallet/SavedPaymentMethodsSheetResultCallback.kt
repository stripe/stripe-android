package com.stripe.android.paymentsheet.wallet

import androidx.annotation.RestrictTo
import com.stripe.android.ExperimentalSavedPaymentMethodsApi

@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface SavedPaymentMethodsSheetResultCallback {
    fun onResult(result: SavedPaymentMethodsSheetResult?)
}
