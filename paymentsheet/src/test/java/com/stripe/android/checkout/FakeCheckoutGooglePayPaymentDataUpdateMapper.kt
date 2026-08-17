package com.stripe.android.checkout

import app.cash.turbine.Turbine
import com.stripe.android.common.model.CommonConfiguration
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
        configuration: CommonConfiguration,
        response: CheckoutSessionResponse,
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse {
        toResponseCalls.add(
            ToResponseCall(
                configuration = configuration,
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
        val configuration: CommonConfiguration,
        val response: CheckoutSessionResponse,
        val paymentDataUpdate: GooglePayPaymentDataUpdate,
    )
}
