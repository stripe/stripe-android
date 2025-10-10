package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ConfirmationTokenClientContextParamsTest {

    @Test
    fun toParamMap_withAllProperties_shouldCreateExpectedMap() {
        val paymentMethodOptions = PaymentMethodOptionsParams.Card(
            network = "visa",
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            setupFutureUsage = "off_session",
            captureMethod = "automatic_async",
            paymentMethodTypes = listOf("card", "apple_pay"),
            onBehalfOf = "acct_123456",
            paymentMethodConfiguration = "pmc_123456",
            customer = "cus_123456",
            paymentMethodOptions = paymentMethodOptions
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mode" to "payment",
                    "currency" to "usd",
                    "setup_future_usage" to "off_session",
                    "capture_method" to "automatic_async",
                    "payment_method_types" to listOf("card", "apple_pay"),
                    "on_behalf_of" to "acct_123456",
                    "payment_method_configuration" to "pmc_123456",
                    "customer" to "cus_123456",
                    "payment_method_options" to mapOf(
                        "card" to mapOf(
                            "network" to "visa",
                            "setup_future_usage" to "off_session"
                        )
                    )
                )
            )
    }

    @Test
    fun toParamMap_withMinimalProperties_shouldCreateExpectedMap() {
        val params = ConfirmationTokenClientContextParams(
            mode = "setup",
            currency = "eur"
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mode" to "setup",
                    "currency" to "eur"
                )
            )
    }

    @Test
    fun toParamMap_withNullProperties_shouldOnlyIncludeNonNullProperties() {
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            setupFutureUsage = null,
            captureMethod = null,
            paymentMethodTypes = null,
            onBehalfOf = null,
            paymentMethodConfiguration = null,
            customer = null,
            paymentMethodOptions = null
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mode" to "payment",
                    "currency" to "usd"
                )
            )
    }

    @Test
    fun toParamMap_withEmptyPaymentMethodOptions_shouldNotIncludePaymentMethodOptions() {
        val paymentMethodOptions = PaymentMethodOptionsParams.Card()
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            paymentMethodOptions = paymentMethodOptions
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mode" to "payment",
                    "currency" to "usd"
                )
            )
    }

    @Test
    fun toParamMap_withSepaDebitPaymentMethodOptions_shouldCreateExpectedMap() {
        val paymentMethodOptions = PaymentMethodOptionsParams.SepaDebit(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "eur",
            setupFutureUsage = "off_session",
            captureMethod = "automatic",
            paymentMethodTypes = listOf("sepa_debit"),
            customer = "cus_test_customer",
            paymentMethodOptions = paymentMethodOptions
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mode" to "payment",
                    "currency" to "eur",
                    "setup_future_usage" to "off_session",
                    "capture_method" to "automatic",
                    "payment_method_types" to listOf("sepa_debit"),
                    "customer" to "cus_test_customer",
                    "payment_method_options" to mapOf(
                        "sepa_debit" to mapOf(
                            "setup_future_usage" to "off_session"
                        )
                    )
                )
            )
    }
}