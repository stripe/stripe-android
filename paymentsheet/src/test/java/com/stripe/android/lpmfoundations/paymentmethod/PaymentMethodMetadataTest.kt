package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.elements.SharedDataSpec
import org.junit.Test

internal class PaymentMethodMetadataTest {
    @Test
    fun `hasIntentToSetup returns true for setup_intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
        )
        assertThat(metadata.hasIntentToSetup()).isTrue()
    }

    @Test
    fun `hasIntentToSetup returns true for payment_intent with setup_future_usage`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession,
            ),
        )
        assertThat(metadata.hasIntentToSetup()).isTrue()
    }

    @Test
    fun `hasIntentToSetup returns false for payment_intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )
        assertThat(metadata.hasIntentToSetup()).isFalse()
    }

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
}
