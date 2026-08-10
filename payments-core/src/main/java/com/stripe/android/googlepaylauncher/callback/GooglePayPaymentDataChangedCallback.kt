package com.stripe.android.googlepaylauncher.callback

/**
 * Callback invoked when the customer changes their shipping address or shipping option in the
 * Google Pay payment sheet.
 */
fun interface GooglePayPaymentDataChangedCallback {
    suspend fun onPaymentDataChanged(
        intermediatePaymentData: GooglePayIntermediatePaymentData,
    ): GooglePayPaymentDataRequestUpdate
}
