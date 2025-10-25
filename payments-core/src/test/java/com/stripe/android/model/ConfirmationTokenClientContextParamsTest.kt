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
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
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
    fun toParamMap_withEmptyPaymentMethodTypes_shouldNotIncludePaymentTypes() {
        val paymentMethodTypes = listOf<String>()
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            paymentMethodTypes = paymentMethodTypes,
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
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
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

    @Test
    fun toParamMap_withRequireCvcRecollection_shouldCreateExpectedMap() {
        val paymentMethodOptions = PaymentMethodOptionsParams.Card(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            paymentMethodOptions = paymentMethodOptions,
            requireCvcRecollection = true
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mode" to "payment",
                    "currency" to "usd",
                    "payment_method_options" to mapOf(
                        "card" to mapOf(
                            "setup_future_usage" to "off_session",
                            "require_cvc_recollection" to true
                        )
                    )
                )
            )
    }

    @Test
    fun toParamMap_withBlankSetupFutureUsage_shouldNotIncludeSetupFutureUsage() {
        val paymentMethodOptions = PaymentMethodOptionsParams.Card(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
        )
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
    fun toParamMap_withOnlyPmoSetupFutureUsage_shouldIncludePmoSfuValues() {
        val paymentMethodOptions = PaymentMethodOptionsParams.Card(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession,
        )
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            paymentMethodOptions = paymentMethodOptions
        )

        val paramMap = params.toParamMap()
        val pmoMap = paramMap["payment_method_options"] as? Map<*, *>
        val cardOptions = pmoMap?.get("card") as? Map<*, *>
        assertThat(cardOptions?.get("setup_future_usage")).isEqualTo("on_session")
    }

    @Test
    fun toParamMap_withPmoSetupFutureUsage_shouldIncludeBothValues() {
        val paymentMethodOptions = PaymentMethodOptionsParams.Card(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            paymentMethodOptions = paymentMethodOptions
        )

        val paramMap = params.toParamMap()
        // Both top-level and PMO should have the same value
        assertThat(paramMap["setup_future_usage"]).isEqualTo("off_session")
        val pmoMap = paramMap["payment_method_options"] as? Map<*, *>
        val cardOptions = pmoMap?.get("card") as? Map<*, *>
        assertThat(cardOptions?.get("setup_future_usage")).isEqualTo("off_session")
    }

    @Test
    fun toParamMap_withPmoBlankSetupFutureUsage_shouldNotIncludeSetupFutureUsage() {
        val paymentMethodOptions = PaymentMethodOptionsParams.Card(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
        )
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank,
            paymentMethodOptions = paymentMethodOptions
        )

        val paramMap = params.toParamMap()
        // Blank values should not be included anywhere
        assertThat(paramMap.containsKey("setup_future_usage")).isFalse()
        val pmoMap = paramMap["payment_method_options"] as? Map<*, *>
        val cardOptions = pmoMap?.get("card") as? Map<*, *>
        assertThat(cardOptions?.containsKey("setup_future_usage")).isNotEqualTo(true)
    }

    @Test
    fun toParamMap_withRequireCvcRecollectionOnly_shouldCreateExpectedMap() {
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            requireCvcRecollection = true
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mode" to "payment",
                    "currency" to "usd",
                    "payment_method_options" to mapOf(
                        "card" to mapOf(
                            "require_cvc_recollection" to true
                        )
                    )
                )
            )
    }

    @Test
    fun toParamMap_withRequireCvcRecollectionAndNonCardPmo_shouldCreateSeparateCardEntry() {
        val paymentMethodOptions = PaymentMethodOptionsParams.SepaDebit(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "eur",
            paymentMethodOptions = paymentMethodOptions,
            requireCvcRecollection = true
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mode" to "payment",
                    "currency" to "eur",
                    "payment_method_options" to mapOf(
                        "sepa_debit" to mapOf(
                            "setup_future_usage" to "off_session"
                        ),
                        "card" to mapOf(
                            "require_cvc_recollection" to true
                        )
                    )
                )
            )
    }

    @Test
    fun toParamMap_withRequireCvcRecollectionFalse_shouldNotIncludeCvcRecollection() {
        val params = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            requireCvcRecollection = false
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mode" to "payment",
                    "currency" to "usd"
                )
            )
    }
}
