package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.paymentelement.CheckoutSessionPreview

@OptIn(CheckoutSessionPreview::class)
internal class FakeCheckoutGooglePayPaymentDataUpdateAddressBuilder(
    private val result: CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result,
) : CheckoutGooglePayPaymentDataUpdateAddressBuilder {
    val buildCalls = Turbine<GooglePayPaymentDataUpdate>()

    override fun build(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result {
        buildCalls.add(paymentDataUpdate)
        return result
    }

    fun ensureAllEventsConsumed() {
        buildCalls.ensureAllEventsConsumed()
    }
}
