package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ConfirmationTokenParamsTest {

    @Test
    fun toParamMap_withPaymentMethodId_shouldCreateExpectedMap() {
        val params = ConfirmationTokenParams(
            paymentMethodId = "pm_12345"
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "payment_method" to "pm_12345"
                )
            )
    }

    @Test
    fun toParamMap_withPaymentMethodData_shouldCreateExpectedMap() {
        val paymentMethodData = PaymentMethodCreateParamsFixtures.DEFAULT_CARD
        val params = ConfirmationTokenParams(
            paymentMethodData = paymentMethodData
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "payment_method_data" to paymentMethodData.toParamMap()
                )
            )
    }

    @Test
    fun toParamMap_withReturnUrl_shouldCreateExpectedMap() {
        val params = ConfirmationTokenParams(
            returnUrl = "https://example.com/return"
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "return_url" to "https://example.com/return"
                )
            )
    }

    @Test
    fun toParamMap_withSetUpFutureUsage_shouldCreateExpectedMap() {
        val params = ConfirmationTokenParams(
            setUpFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "setup_future_usage" to "on_session"
                )
            )
    }

    @Test
    fun toParamMap_withShipping_shouldCreateExpectedMap() {
        val shipping = ConfirmPaymentIntentParams.Shipping(
            address = AddressFixtures.ADDRESS,
            name = "Jenny Rosen"
        )
        val params = ConfirmationTokenParams(
            shipping = shipping
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "shipping" to shipping.toParamMap()
                )
            )
    }

    @Test
    fun toParamMap_withMandateDataParams_shouldCreateExpectedMap() {
        val mandateData = MandateDataParamsFixtures.DEFAULT
        val params = ConfirmationTokenParams(
            mandateDataParams = mandateData
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "mandate_data" to mandateData.toParamMap()
                )
            )
    }

    @Test
    fun toParamMap_withSetAsDefaultPaymentMethod_shouldCreateExpectedMap() {
        val params = ConfirmationTokenParams(
            setAsDefaultPaymentMethod = true
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "set_as_default_payment_method" to true
                )
            )
    }

    @Test
    fun toParamMap_withAllParameters_shouldCreateExpectedMap() {
        val paymentMethodData = PaymentMethodCreateParamsFixtures.DEFAULT_CARD
        val shipping = ConfirmPaymentIntentParams.Shipping(
            address = AddressFixtures.ADDRESS,
            name = "Jenny Rosen",
            phone = "555-123-4567"
        )
        val mandateData = MandateDataParamsFixtures.DEFAULT

        val params = ConfirmationTokenParams(
            paymentMethodId = "pm_12345",
            paymentMethodData = paymentMethodData,
            returnUrl = "https://example.com/return",
            setUpFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            shipping = shipping,
            mandateDataParams = mandateData,
            setAsDefaultPaymentMethod = true
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "payment_method" to "pm_12345",
                    "payment_method_data" to paymentMethodData.toParamMap(),
                    "return_url" to "https://example.com/return",
                    "setup_future_usage" to "off_session",
                    "shipping" to shipping.toParamMap(),
                    "mandate_data" to mandateData.toParamMap(),
                    "set_as_default_payment_method" to true
                )
            )
    }

    @Test
    fun toParamMap_withNullParameters_shouldCreateEmptyMap() {
        val params = ConfirmationTokenParams()

        assertThat(params.toParamMap())
            .isEmpty()
    }

    @Test
    fun toParamMap_withFalseSetAsDefaultPaymentMethod_shouldCreateExpectedMap() {
        val params = ConfirmationTokenParams(
            setAsDefaultPaymentMethod = false
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "set_as_default_payment_method" to false
                )
            )
    }

    @Test
    fun toParamMap_withBlankSetupFutureUsage_shouldCreateExpectedMap() {
        val params = ConfirmationTokenParams(
            setUpFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "setup_future_usage" to ""
                )
            )
    }

    @Test
    fun toParamMap_withPaymentMethodCreateParamsRequiringMandate_shouldIncludeAllParameters() {
        val sepaDebitParams = PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT
        val params = ConfirmationTokenParams(
            paymentMethodData = sepaDebitParams,
            mandateDataParams = MandateDataParamsFixtures.DEFAULT
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "payment_method_data" to sepaDebitParams.toParamMap(),
                    "mandate_data" to MandateDataParamsFixtures.DEFAULT.toParamMap()
                )
            )
    }

    @Test
    fun toParamMap_withComplexShipping_shouldCreateExpectedMap() {
        val shipping = ConfirmPaymentIntentParams.Shipping(
            address = AddressFixtures.ADDRESS,
            name = "Jenny Rosen",
            carrier = "FedEx",
            phone = "555-123-4567",
            trackingNumber = "1234567890"
        )
        val params = ConfirmationTokenParams(
            shipping = shipping
        )

        val expectedShippingMap = mapOf(
            "address" to AddressFixtures.ADDRESS.toParamMap(),
            "name" to "Jenny Rosen",
            "carrier" to "FedEx",
            "phone" to "555-123-4567",
            "tracking_number" to "1234567890"
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "shipping" to expectedShippingMap
                )
            )
    }

    @Test
    fun toParamMap_withClientContext_shouldCreateExpectedMap() {
        val clientContext = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            setupFutureUsage = "off_session",
            captureMethod = "automatic"
        )
        val params = ConfirmationTokenParams(
            clientContext = clientContext
        )

        assertThat(params.toParamMap())
            .isEqualTo(
                mapOf(
                    "client_context" to clientContext.toParamMap()
                )
            )
    }

    @Test
    fun toParamMap_withComprehensiveClientContext_shouldCreateExpectedMap() {
        val paymentMethodOptions = PaymentMethodOptionsParams.Card(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )
        val clientContext = ConfirmationTokenClientContextParams(
            mode = "payment",
            currency = "usd",
            setupFutureUsage = "on_session",
            captureMethod = "automatic",
            paymentMethodTypes = listOf("card", "apple_pay"),
            onBehalfOf = "acct_123",
            paymentMethodConfiguration = "pmc_123",
            customer = "cus_123",
            paymentMethodOptions = paymentMethodOptions
        )
        val params = ConfirmationTokenParams(
            paymentMethodData = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            returnUrl = "https://example.com/return",
            clientContext = clientContext
        )

        val paramMap = params.toParamMap()
        assertThat(paramMap["payment_method_data"]).isNotNull()
        assertThat(paramMap["return_url"]).isEqualTo("https://example.com/return")

        val clientContextMap = paramMap["client_context"] as Map<*, *>
        assertThat(clientContextMap["mode"]).isEqualTo("payment")
        assertThat(clientContextMap["currency"]).isEqualTo("usd")
        assertThat(clientContextMap["setup_future_usage"]).isEqualTo("on_session")
        assertThat(clientContextMap["capture_method"]).isEqualTo("automatic")
        assertThat(clientContextMap["payment_method_types"]).isEqualTo(listOf("card", "apple_pay"))
        assertThat(clientContextMap["on_behalf_of"]).isEqualTo("acct_123")
        assertThat(clientContextMap["payment_method_configuration"]).isEqualTo("pmc_123")
        assertThat(clientContextMap["customer"]).isEqualTo("cus_123")
        assertThat(clientContextMap["payment_method_options"]).isNotNull()
    }

    @Test
    fun toParamMap_withoutClientContext_shouldNotIncludeClientContext() {
        val params = ConfirmationTokenParams(
            paymentMethodData = PaymentMethodCreateParamsFixtures.DEFAULT_CARD
        )

        assertThat(params.toParamMap().containsKey("client_context")).isFalse()
    }
}
