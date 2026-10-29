package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test

class ClientAttributionMetadataKtxTest {

    @Test
    fun `payment intent creation flow is standard for payment intent`() {
        val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("cs_123")

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
        )

        assertThat(clientAttributionMetadata.paymentIntentCreationFlow)
            .isEqualTo(PaymentIntentCreationFlow.Standard)
    }

    @Test
    fun `payment intent creation flow is standard for setup intent`() {
        val initializationMode = PaymentElementLoader.InitializationMode.SetupIntent("cs_123")

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
        )

        assertThat(clientAttributionMetadata.paymentIntentCreationFlow)
            .isEqualTo(PaymentIntentCreationFlow.Standard)
    }

    @Test
    fun `payment intent creation flow is deferred for deferred intent`() {
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 100L,
                    currency = "usd",
                )
            )
        )

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
        )

        assertThat(clientAttributionMetadata.paymentIntentCreationFlow)
            .isEqualTo(PaymentIntentCreationFlow.Deferred)
    }

    @Test
    fun `elements session config ID is set properly`() {
        val expectedElementsSessionConfigId = "e961790f-43ed-4fcc-a534-74eeca28d042"

        val clientAttributionMetadata = createClientAttributionMetadata(
            elementsSessionConfigId = expectedElementsSessionConfigId,
        )

        assertThat(clientAttributionMetadata.elementsSessionConfigId).isEqualTo(
            expectedElementsSessionConfigId
        )
    }

    @Test
    fun `payment method selection flow is automatic when automatic payment methods are enabled`() {
        val clientAttributionMetadata = createClientAttributionMetadata(
            automaticPaymentMethodsEnabled = true,
        )

        assertThat(clientAttributionMetadata.paymentMethodSelectionFlow).isEqualTo(
            PaymentMethodSelectionFlow.Automatic
        )
    }

    @Test
    fun `payment method selection flow is merchant specified when automatic payment methods are not enabled`() {
        val clientAttributionMetadata = createClientAttributionMetadata(
            automaticPaymentMethodsEnabled = false,
        )

        assertThat(clientAttributionMetadata.paymentMethodSelectionFlow).isEqualTo(
            PaymentMethodSelectionFlow.MerchantSpecified
        )
    }

    @Test
    fun `payment intent creation flow is null for checkout session`() {
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            collectedEmail = null,
            instancesKey = "instances_key",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(id = "cs_test_456"),
            configuredElements = emptyList(),
        )

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
        )

        assertThat(clientAttributionMetadata.paymentIntentCreationFlow).isNull()
    }

    @Test
    fun `checkout session ID is set for checkout session`() {
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            collectedEmail = null,
            instancesKey = "instances_key",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(id = "cs_test_456"),
            configuredElements = emptyList(),
        )

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
        )

        assertThat(clientAttributionMetadata.checkoutSessionId).isEqualTo("cs_test_456")
    }

    @Test
    fun `checkout session ID is null for non-checkout-session modes`() {
        val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("cs_123")

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
        )

        assertThat(clientAttributionMetadata.checkoutSessionId).isNull()
    }

    @Test
    fun `checkout session metadata includes checkout attribution and configured elements`() {
        val configuredElements = listOf(
            "mobile-payment-element",
            "mobile-express-checkout-element",
            "mobile-shipping-address-element",
        )
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            collectedEmail = null,
            instancesKey = "instances_key",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                id = "cs_test_456",
                configId = "cfg_test_123",
                automaticPaymentMethodTypes = true,
            ),
            configuredElements = configuredElements,
        )

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
        )
        val params = clientAttributionMetadata.toParamMap()

        assertThat(clientAttributionMetadata.paymentIntentCreationFlow).isNull()
        assertThat(clientAttributionMetadata.paymentMethodSelectionFlow)
            .isEqualTo(PaymentMethodSelectionFlow.Automatic)
        assertThat(clientAttributionMetadata.checkoutSessionId).isEqualTo("cs_test_456")
        assertThat(params["merchant_integration_source"]).isEqualTo("mobile-checkout")
        assertThat(params["merchant_integration_subtype"]).isEqualTo("mobile-payment-element")
        assertThat(params["checkout_config_id"]).isEqualTo("cfg_test_123")
        assertThat(params["merchant_integration_additional_elements"]).isEqualTo(configuredElements)
    }

    @Test
    fun `checkout session metadata uses express checkout subtype for express checkout element`() {
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            collectedEmail = null,
            instancesKey = "instances_key",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(id = "cs_test_456"),
            configuredElements = listOf("mobile-express-checkout-element"),
        )

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
            isExpressCheckoutElement = true,
        )

        assertThat(clientAttributionMetadata.toParamMap()["merchant_integration_subtype"])
            .isEqualTo("mobile-express-checkout-element")
    }

    @Test
    fun `checkout session payment method selection flow is merchant specified when response says false`() {
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            collectedEmail = null,
            instancesKey = "instances_key",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticPaymentMethodTypes = false,
            ),
            configuredElements = listOf("mobile-payment-element"),
        )

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
            automaticPaymentMethodsEnabled = true,
        )

        assertThat(clientAttributionMetadata.paymentMethodSelectionFlow)
            .isEqualTo(PaymentMethodSelectionFlow.MerchantSpecified)
    }

    @Test
    fun `checkout session payment method selection flow is omitted when response omits automatic types`() {
        val initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            collectedEmail = null,
            instancesKey = "instances_key",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            configuredElements = listOf("mobile-payment-element"),
        )

        val clientAttributionMetadata = createClientAttributionMetadata(
            initializationMode = initializationMode,
            automaticPaymentMethodsEnabled = true,
        )

        assertThat(clientAttributionMetadata.paymentMethodSelectionFlow).isNull()
        assertThat(clientAttributionMetadata.toParamMap()).doesNotContainKey("payment_method_selection_flow")
    }

    private fun createClientAttributionMetadata(
        elementsSessionConfigId: String = "e961790f-43ed-4fcc-a534-74eeca28d042",
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.PaymentIntent(
                "cs_123"
            ),
        automaticPaymentMethodsEnabled: Boolean = false,
        isExpressCheckoutElement: Boolean = false,
    ): ClientAttributionMetadata {
        return ClientAttributionMetadata.create(
            elementsSessionConfigId = elementsSessionConfigId,
            initializationMode = initializationMode,
            automaticPaymentMethodsEnabled = automaticPaymentMethodsEnabled,
            isExpressCheckoutElement = isExpressCheckoutElement,
        )
    }
}
