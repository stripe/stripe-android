package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal class FakeCheckoutGooglePayPaymentDataUpdateMapper(
    private val response: GooglePayPaymentDataUpdateResponse,
) : CheckoutGooglePayPaymentDataUpdateMapper {
    val toResponseCalls = Turbine<ToResponseCall>()

    override fun toResponse(
        countryCode: String?,
        response: CheckoutSessionResponse,
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse {
        toResponseCalls.add(
            ToResponseCall(
                countryCode = countryCode,
                response = response,
                paymentDataUpdate = paymentDataUpdate,
            )
        )
        return this.response
    }

    fun ensureAllEventsConsumed() {
        toResponseCalls.ensureAllEventsConsumed()
    }

    data class ToResponseCall(
        val countryCode: String?,
        val response: CheckoutSessionResponse,
        val paymentDataUpdate: GooglePayPaymentDataUpdate,
    )
}
