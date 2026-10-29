package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.version.StripeSdkVersion
import org.junit.Test

class ClientAttributionMetadataTest {

    @Test
    fun `toParamMap() creates correct params`() {
        val clientAttributionMetadataParams = ClientAttributionMetadata(
            elementsSessionConfigId = "e961790f-43ed-4fcc-a534-74eeca28d042",
            paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
            paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
            checkoutSessionId = null,
            checkoutConfigId = null,
            merchantIntegrationSource = "elements",
            merchantIntegrationSubtype = "mobile",
            merchantIntegrationAdditionalElements = null,
        ).toParamMap()

        assertThat(clientAttributionMetadataParams).hasSize(7)
        assertThat(clientAttributionMetadataParams).containsEntry(
            "elements_session_config_id",
            "e961790f-43ed-4fcc-a534-74eeca28d042"
        )
        assertThat(clientAttributionMetadataParams).containsEntry(
            "payment_intent_creation_flow",
            "standard"
        )
        assertThat(clientAttributionMetadataParams).containsEntry(
            "merchant_integration_source",
            "elements"
        )
        assertThat(clientAttributionMetadataParams).containsEntry(
            "merchant_integration_subtype",
            "mobile"
        )
        assertThat(clientAttributionMetadataParams).containsEntry(
            "merchant_integration_version",
            "stripe-android/${StripeSdkVersion.VERSION_NAME}"
        )
        assertThat(clientAttributionMetadataParams).containsEntry(
            "client_session_id",
            AnalyticsRequestFactory.sessionId.toString()
        )
        assertThat(clientAttributionMetadataParams).containsEntry(
            "payment_method_selection_flow",
            "merchant_specified",
        )
    }

    @Test
    fun `toParamMap() omits paymentIntentCreationFlow if null`() {
        val clientAttributionMetadataParams = ClientAttributionMetadata(
            elementsSessionConfigId = "e961790f-43ed-4fcc-a534-74eeca28d042",
            paymentMethodSelectionFlow = PaymentMethodSelectionFlow.Automatic,
            paymentIntentCreationFlow = null,
            checkoutSessionId = null,
            checkoutConfigId = null,
            merchantIntegrationSource = "elements",
            merchantIntegrationSubtype = "mobile",
            merchantIntegrationAdditionalElements = null,
        ).toParamMap()

        assertThat(clientAttributionMetadataParams).hasSize(6)
        assertThat(clientAttributionMetadataParams).doesNotContainKey("payment_intent_creation_flow")
    }

    @Test
    fun `toParamMap() omits paymentMethodSelectionFlow if null`() {
        val clientAttributionMetadataParams = ClientAttributionMetadata(
            elementsSessionConfigId = "e961790f-43ed-4fcc-a534-74eeca28d042",
            paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
            paymentMethodSelectionFlow = null,
            checkoutSessionId = null,
            checkoutConfigId = null,
            merchantIntegrationSource = "elements",
            merchantIntegrationSubtype = "mobile",
            merchantIntegrationAdditionalElements = null,
        ).toParamMap()

        assertThat(clientAttributionMetadataParams).hasSize(6)
        assertThat(clientAttributionMetadataParams).doesNotContainKey("payment_method_selection_flow")
    }

    @Test
    fun `toParamMap() omits elementsSessionConfigId if null`() {
        val clientAttributionMetadataParams = ClientAttributionMetadata(
            elementsSessionConfigId = null,
            paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
            paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
            checkoutSessionId = null,
            checkoutConfigId = null,
            merchantIntegrationSource = "elements",
            merchantIntegrationSubtype = "mobile",
            merchantIntegrationAdditionalElements = null,
        ).toParamMap()

        assertThat(clientAttributionMetadataParams).hasSize(6)
        assertThat(clientAttributionMetadataParams).doesNotContainKey("elements_session_config_id")
    }

    @Test
    fun `toParamMap() includes checkout attribution when present`() {
        val clientAttributionMetadataParams = ClientAttributionMetadata(
            elementsSessionConfigId = null,
            paymentIntentCreationFlow = null,
            paymentMethodSelectionFlow = PaymentMethodSelectionFlow.Automatic,
            checkoutSessionId = "cs_test_123",
            checkoutConfigId = "checkout_config_123",
            merchantIntegrationSource = "mobile-checkout",
            merchantIntegrationSubtype = "mobile-payment-element",
            merchantIntegrationAdditionalElements = listOf("mobile-payment-element", "mobile-shipping-address-element"),
        ).toParamMap()

        assertThat(clientAttributionMetadataParams).containsEntry("checkout_session_id", "cs_test_123")
        assertThat(clientAttributionMetadataParams).containsEntry("checkout_config_id", "checkout_config_123")
        assertThat(clientAttributionMetadataParams).containsEntry("merchant_integration_source", "mobile-checkout")
        assertThat(clientAttributionMetadataParams).containsEntry(
            "merchant_integration_subtype",
            "mobile-payment-element",
        )
        assertThat(clientAttributionMetadataParams).containsEntry(
            "merchant_integration_additional_elements",
            listOf("mobile-payment-element", "mobile-shipping-address-element"),
        )
    }

    @Test
    fun `toParamMap() omits checkoutSessionId if null`() {
        val clientAttributionMetadataParams = ClientAttributionMetadata(
            elementsSessionConfigId = null,
            paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
            paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
            checkoutSessionId = null,
            checkoutConfigId = null,
            merchantIntegrationSource = "elements",
            merchantIntegrationSubtype = "mobile",
            merchantIntegrationAdditionalElements = null,
        ).toParamMap()

        assertThat(clientAttributionMetadataParams).doesNotContainKey("checkout_session_id")
    }
}
