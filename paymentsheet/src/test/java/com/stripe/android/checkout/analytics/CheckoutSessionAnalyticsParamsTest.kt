package com.stripe.android.checkout.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test

internal class CheckoutSessionAnalyticsParamsTest {
    @Test
    fun `maps checkout fields and excludes response request id`() {
        val response = CheckoutSessionResponseFactory.create(
            id = "cs_test_123",
            configId = "cfg_checkout_123",
            requestId = "req_stale",
            currency = "cad",
            paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            status = CheckoutSessionResponse.Status.COMPLETE,
            adaptivePricingInfo = adaptivePricingInfo(
                activePresentmentCurrency = "eur",
                integrationCurrency = "cad",
            ),
            automaticTaxEnabled = true,
        )

        assertThat(response.checkoutAnalyticsParams()).containsExactlyEntriesIn(
            mapOf(
                "checkout_session_id" to "cs_test_123",
                "checkout_config_id" to "cfg_checkout_123",
                "currency" to "cad",
                "session_status" to "complete",
                "payment_status" to "paid",
                "adaptive_pricing_active" to true,
                "automatic_tax_enabled" to true,
            )
        )
    }

    @Test
    fun `omits absent optional identifiers and inactive adaptive pricing`() {
        val response = CheckoutSessionResponseFactory.create(requestId = "req_stale")

        assertThat(response.checkoutAnalyticsParams()).containsExactlyEntriesIn(
            mapOf(
                "checkout_session_id" to response.id,
                "currency" to "usd",
                "session_status" to "open",
                "payment_status" to "unpaid",
                "adaptive_pricing_active" to false,
                "automatic_tax_enabled" to false,
            )
        )
    }

    @Test
    fun `adaptive pricing is inactive when presentment matches integration currency`() {
        val response = CheckoutSessionResponseFactory.create(
            adaptivePricingInfo = adaptivePricingInfo(
                activePresentmentCurrency = "usd",
                integrationCurrency = "usd",
            ),
        )

        assertThat(response.checkoutAnalyticsParams())
            .containsEntry("adaptive_pricing_active", false)
    }

    private fun adaptivePricingInfo(
        activePresentmentCurrency: String,
        integrationCurrency: String,
    ) = CheckoutSessionResponse.AdaptivePricingInfo(
        activePresentmentCurrency = activePresentmentCurrency,
        integrationAmount = 1000,
        integrationCurrency = integrationCurrency,
        localCurrencyOptions = emptyList(),
    )
}
