package com.stripe.android.paymentsheet

/**
 * Callback that is invoked when a [ConfirmationTokenResult] is available.
 *
 * This callback is used when PaymentSheet is configured to create ConfirmationTokens
 * instead of completing payment confirmation directly.
 */
fun interface ConfirmationTokenCallback {
    fun onConfirmationTokenResult(confirmationTokenResult: ConfirmationTokenResult)
}
