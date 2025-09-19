package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test

class ClientAttributionMetadataKtxKtTest {

    @Test
    fun `payment intent creation flow is standard for payment intent`() {
        val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("cs_123")

        val clientAttributionMetadata = ClientAttributionMetadata.create(
            elementsSessionConfigId = "elements_session_123",
            initializationMode = initializationMode,
        )

        assertThat(clientAttributionMetadata.paymentIntentCreationFlow)
            .isEqualTo(PaymentIntentCreationFlow.Standard)
    }

    @Test
    fun `payment intent creation flow is standard for setup intent`() {
        val initializationMode = PaymentElementLoader.InitializationMode.SetupIntent("cs_123")

        val clientAttributionMetadata = ClientAttributionMetadata.create(
            elementsSessionConfigId = "elements_session_123",
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

        val clientAttributionMetadata = ClientAttributionMetadata.create(
            elementsSessionConfigId = "elements_session_123",
            initializationMode = initializationMode,
        )

        assertThat(clientAttributionMetadata.paymentIntentCreationFlow)
            .isEqualTo(PaymentIntentCreationFlow.Deferred)
    }

    @Test
    fun `elements session config ID is set properly`() {
        val expectedElementsSessionConfigId = "elements_session_123"

        val clientAttributionMetadata = ClientAttributionMetadata.create(
            elementsSessionConfigId = expectedElementsSessionConfigId,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                "cs_123"
            ),
        )

        assertThat(clientAttributionMetadata.elementsSessionConfigId).isEqualTo(
            expectedElementsSessionConfigId
        )
    }
}
