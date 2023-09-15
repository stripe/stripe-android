package com.stripe.android.customersheet

/**
 * Callback to be used when you use [CustomerSheet], called when a customer makes a payment method
 * selection, the sheet is canceled, or an error occurred.
 */
@ExperimentalCustomerSheetApi
fun interface CustomerSheetResultCallback {
    fun onCustomerSheetResult(result: CustomerSheetResult)
}
