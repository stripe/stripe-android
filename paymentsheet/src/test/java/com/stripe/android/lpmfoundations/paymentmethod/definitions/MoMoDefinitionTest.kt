package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

internal class MoMoDefinitionTest {
    @Test
    fun `does not require the merchant delayed payment setting`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("momo")),
            allowsDelayedPaymentMethods = false,
        )

        assertThat(MoMoDefinition.isSupported(metadata)).isTrue()
    }

    @Test
    fun `supports PaymentIntents without native authorization or mandate UI`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.MoMo),
        )

        assertThat(MoMoDefinition.isSupported(metadata)).isTrue()
        assertThat(MoMoDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `does not support PaymentIntents with setup future usage`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage
                .stripeIntent(PaymentMethod.Type.MoMo),
        )

        assertThat(MoMoDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `does not support SetupIntents`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent
                .stripeIntent(PaymentMethod.Type.MoMo),
        )

        assertThat(MoMoDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `requirements exclude intents with setup`() {
        assertThat(MoMoDefinition.requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup = false))
            .containsExactly(AddPaymentMethodRequirement.UnsupportedForSetup)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(MoMoDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
