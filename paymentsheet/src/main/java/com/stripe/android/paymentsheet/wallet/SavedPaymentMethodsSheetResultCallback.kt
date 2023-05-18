package com.stripe.android.paymentsheet.wallet

import androidx.annotation.RestrictTo
import com.stripe.android.ExperimentalSavedPaymentMethodsApi

/**
 * Callback to be used when you use [SavedPaymentMethodsSheet], called when a customer has either
 * made a payment method selection, canceled, or an error occurred.
 */
@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface SavedPaymentMethodsSheetResultCallback {
    fun onResult(result: SavedPaymentMethodsSheetResult?)
}
