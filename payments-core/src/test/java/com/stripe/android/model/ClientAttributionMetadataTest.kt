package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.version.StripeSdkVersion
import org.junit.Test

class ClientAttributionMetadataTest {

    @Test
    fun `toParamMap() creates correct params`() {
        val clientAttributionMetadataParams = ClientAttributionMetadata(
            elementsSessionConfigId = "elements_session_123",
            paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
        ).toParamMap()

        assertThat(clientAttributionMetadataParams).hasSize(6)
        assertThat(clientAttributionMetadataParams).containsEntry(
            "elements_session_config_id",
            "elements_session_123"
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
    }
}
