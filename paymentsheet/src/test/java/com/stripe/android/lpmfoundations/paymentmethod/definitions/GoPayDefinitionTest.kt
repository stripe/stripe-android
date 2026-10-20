package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import org.junit.Test

internal class GoPayDefinitionTest {
    @Test
    fun `supports PaymentIntents without native authorization or mandate UI`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.GoPay),
        )

        assertThat(GoPayDefinition.isSupported(metadata)).isTrue()
        assertThat(GoPayDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `does not support PaymentIntents with setup future usage`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage
                .stripeIntent(PaymentMethod.Type.GoPay),
        )

        assertThat(GoPayDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `does not support SetupIntents`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent
                .stripeIntent(PaymentMethod.Type.GoPay),
        )

        assertThat(GoPayDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `requirements exclude intents with setup`() {
        assertThat(GoPayDefinition.requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup = false))
            .containsExactly(AddPaymentMethodRequirement.UnsupportedForSetup)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(GoPayDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
