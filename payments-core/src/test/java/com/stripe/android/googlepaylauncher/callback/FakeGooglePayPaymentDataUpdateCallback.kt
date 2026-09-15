package com.stripe.android.googlepaylauncher.callback

import app.cash.turbine.Turbine
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse

internal class FakeGooglePayPaymentDataUpdateCallback(
    private val result: (GooglePayPaymentDataUpdate) -> GooglePayPaymentDataUpdateResponse,
) : GooglePayPaymentDataUpdateCallback {
    val updates = Turbine<GooglePayPaymentDataUpdate>()

    override suspend fun onPaymentDataChanged(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse {
        updates.add(paymentDataUpdate)
        return result(paymentDataUpdate)
    }

    fun ensureAllEventsConsumed() {
        updates.ensureAllEventsConsumed()
    }
}
