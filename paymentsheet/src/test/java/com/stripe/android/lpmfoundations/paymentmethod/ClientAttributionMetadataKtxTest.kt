package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.paymentsheet.PaymentSheet
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
        val expectedElementsSessionConfigId = "elements_session_123"

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

    private fun createClientAttributionMetadata(
        elementsSessionConfigId: String = "elements_session_123",
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.PaymentIntent(
                "cs_123"
            ),
        automaticPaymentMethodsEnabled: Boolean = false
    ): ClientAttributionMetadata {
        return ClientAttributionMetadata.create(
            elementsSessionConfigId = elementsSessionConfigId,
            initializationMode = initializationMode,
            automaticPaymentMethodsEnabled = automaticPaymentMethodsEnabled,
        )
    }
}
