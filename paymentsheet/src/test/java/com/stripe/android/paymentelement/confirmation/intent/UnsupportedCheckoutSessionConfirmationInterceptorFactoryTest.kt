package com.stripe.android.paymentelement.confirmation.intent

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import kotlin.test.assertFailsWith

internal class UnsupportedCheckoutSessionConfirmationInterceptorFactoryTest {
    @Test
    fun `rejects Checkout confirmation outside CheckoutController`() {
        val response = CheckoutSessionResponseFactory.create()
        val error = assertFailsWith<IllegalStateException> {
            UnsupportedCheckoutSessionConfirmationInterceptorFactory().create(
                integrationMetadata = IntegrationMetadata.CheckoutSession(
                    id = response.id,
                    instancesKey = "test_key",
                    checkoutSessionResponse = response,
                ),
                customerMetadata = null,
                clientAttributionMetadata = PaymentMethodMetadataFactory.create().clientAttributionMetadata,
            )
        }
        assertThat(error).hasMessageThat()
            .isEqualTo("Checkout Session confirmation is only supported by CheckoutController.")
    }
}
