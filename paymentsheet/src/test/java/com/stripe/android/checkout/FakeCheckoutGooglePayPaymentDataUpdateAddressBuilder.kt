package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal class FakeCheckoutGooglePayPaymentDataUpdateAddressBuilder(
    private val result: (CheckoutSessionResponse.TaxAddressSource) ->
        CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result,
) : CheckoutGooglePayPaymentDataUpdateAddressBuilder {
    val buildCalls = Turbine<BuildCall>()

    override fun build(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
        addressSource: CheckoutSessionResponse.TaxAddressSource,
    ): CheckoutGooglePayPaymentDataUpdateAddressBuilder.Result {
        buildCalls.add(BuildCall(paymentDataUpdate, addressSource))
        return result(addressSource)
    }

    fun ensureAllEventsConsumed() {
        buildCalls.ensureAllEventsConsumed()
    }

    data class BuildCall(
        val paymentDataUpdate: GooglePayPaymentDataUpdate,
        val addressSource: CheckoutSessionResponse.TaxAddressSource,
    )
}
