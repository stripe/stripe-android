package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.definitions.GrabPayDefinition
import com.stripe.android.model.SetupIntentFixtures
import org.junit.Test

internal class PaymentMethodDefinitionTest {

    @Test
    fun `isSupported returns false for PaymentMethodDefinitions that do not meet add requirements`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "grabpay")
            )
        )
        assertThat(GrabPayDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `isSupported returns true for supported PaymentMethodDefinitions`() {
        assertThat(CardDefinition.isSupported(PaymentMethodMetadataFactory.create())).isTrue()
    }
}
