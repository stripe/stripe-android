package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.ui.core.elements.SharedDataSpec
import org.junit.Test

internal class PaymentMethodRegistryTest {
    private val cardSharedDataSpecs = listOf(SharedDataSpec("card"))

    @Test
    fun `filterSupportedPaymentMethods removes unsupported paymentMethodTypes`() {
        val metadata = PaymentMethodMetadataFactory.create()
        val supportedPaymentMethods = PaymentMethodRegistry.filterSupportedPaymentMethods(metadata, cardSharedDataSpecs)
        assertThat(supportedPaymentMethods).hasSize(1)
        assertThat(supportedPaymentMethods.first().code).isEqualTo("card")
    }

    @Test
    fun `filterSupportedPaymentMethods filters payment methods without shared data specs`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            )
        )
        val supportedPaymentMethods = PaymentMethodRegistry.filterSupportedPaymentMethods(metadata, cardSharedDataSpecs)
        assertThat(supportedPaymentMethods).hasSize(1)
        assertThat(supportedPaymentMethods.first().code).isEqualTo("card")
    }

    @Test
    fun `filterSupportedPaymentMethods returns expected items`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna")
            )
        )
        val sharedDataSpecs = listOf(SharedDataSpec("card"), SharedDataSpec("klarna"))
        val supportedPaymentMethods = PaymentMethodRegistry.filterSupportedPaymentMethods(metadata, sharedDataSpecs)
        assertThat(supportedPaymentMethods).hasSize(2)
        assertThat(supportedPaymentMethods[0].code).isEqualTo("card")
        assertThat(supportedPaymentMethods[1].code).isEqualTo("klarna")
    }
}
