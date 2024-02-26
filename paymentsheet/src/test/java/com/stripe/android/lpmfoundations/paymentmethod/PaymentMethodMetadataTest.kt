package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.ui.core.elements.SharedDataSpec
import org.junit.Test

internal class PaymentMethodMetadataTest {
    @Test
    fun `filterSupportedPaymentMethods removes unsupported paymentMethodTypes`() {
        val metadata = PaymentMethodMetadataFactory.create()
        val supportedPaymentMethods = metadata.supportedPaymentMethodDefinitions()
        assertThat(supportedPaymentMethods).hasSize(1)
        assertThat(supportedPaymentMethods.first().type.code).isEqualTo("card")
    }

    @Test
    fun `filterSupportedPaymentMethods filters payment methods without shared data specs`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            )
        )
        val supportedPaymentMethods = metadata.supportedPaymentMethodDefinitions()
        assertThat(supportedPaymentMethods).hasSize(1)
        assertThat(supportedPaymentMethods.first().type.code).isEqualTo("card")
    }

    @Test
    fun `filterSupportedPaymentMethods returns expected items`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            ),
            sharedDataSpecs = listOf(SharedDataSpec("card"), SharedDataSpec("klarna")),
        )
        val supportedPaymentMethods = metadata.supportedPaymentMethodDefinitions()
        assertThat(supportedPaymentMethods).hasSize(2)
        assertThat(supportedPaymentMethods[0].type.code).isEqualTo("card")
        assertThat(supportedPaymentMethods[1].type.code).isEqualTo("klarna")
    }

    @Test
    fun `filterSupportedPaymentMethods filters unactivated payment methods in live mode`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna"),
                unactivatedPaymentMethods = listOf("klarna"),
                isLiveMode = true,
            ),
            sharedDataSpecs = listOf(SharedDataSpec("card"), SharedDataSpec("klarna")),
        )
        val supportedPaymentMethods = metadata.supportedPaymentMethodDefinitions()
        assertThat(supportedPaymentMethods).hasSize(1)
        assertThat(supportedPaymentMethods[0].type.code).isEqualTo("card")
    }

    @Test
    fun `filterSupportedPaymentMethods does not filter unactivated payment methods in test mode`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna"),
                unactivatedPaymentMethods = listOf("klarna"),
                isLiveMode = false,
            ),
            sharedDataSpecs = listOf(SharedDataSpec("card"), SharedDataSpec("klarna")),
        )
        val supportedPaymentMethods = metadata.supportedPaymentMethodDefinitions()
        assertThat(supportedPaymentMethods).hasSize(2)
        assertThat(supportedPaymentMethods[0].type.code).isEqualTo("card")
        assertThat(supportedPaymentMethods[1].type.code).isEqualTo("klarna")
    }

    @Test
    fun `supportedPaymentMethodForCode returns expected result`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("klarna")
            ),
            sharedDataSpecs = listOf(SharedDataSpec("klarna")),
        )
        assertThat(metadata.supportedPaymentMethodForCode("klarna")?.code).isEqualTo("klarna")
    }

    @Test
    fun `supportedPaymentMethodForCode returns null when sharedDataSpecs are missing`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("klarna")
            ),
            sharedDataSpecs = emptyList(),
        )
        assertThat(metadata.supportedPaymentMethodForCode("klarna")).isNull()
    }

    @Test
    fun `supportedPaymentMethodForCode returns null when it's not supported`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card")
            ),
            sharedDataSpecs = listOf(SharedDataSpec("klarna")),
        )
        assertThat(metadata.supportedPaymentMethodForCode("klarna")).isNull()
    }
}
