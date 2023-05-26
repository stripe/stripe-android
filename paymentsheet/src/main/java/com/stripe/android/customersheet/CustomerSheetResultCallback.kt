package com.stripe.android.customersheet

import androidx.annotation.RestrictTo

/**
 * Callback to be used when you use [CustomerSheet], called when a customer makes a payment method
 * selection, the sheet is canceled, or an error occurred.
 */
@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CustomerSheetResultCallback {
    fun onResult(result: CustomerSheetResult)
}
