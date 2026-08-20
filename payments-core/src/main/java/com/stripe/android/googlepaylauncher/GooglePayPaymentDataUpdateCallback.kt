package com.stripe.android.googlepaylauncher

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface GooglePayPaymentDataUpdateCallback {
    suspend fun onPaymentDataChanged(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse
}
