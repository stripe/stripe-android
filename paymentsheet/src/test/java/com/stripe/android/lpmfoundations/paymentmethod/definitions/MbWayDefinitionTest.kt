package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

class MbWayDefinitionTest {
    private val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFactory.create(
            paymentMethodTypes = listOf("mb_way"),
        )
    )

    @Test
    fun `createFormElements requires phone number`() {
        val formElements = MbWayDefinition.formElements(metadata)

        assertThat(formElements).hasSize(1)
        checkPhoneField(formElements, 0)
    }

    @Test
    fun `requirements include unsupported for setup`() {
        assertThat(MbWayDefinition.requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup = false))
            .containsExactly(AddPaymentMethodRequirement.UnsupportedForSetup)
    }

    @Test
    fun `requiresMandate returns false`() {
        assertThat(MbWayDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `supportedAsSavedPaymentMethod is false`() {
        assertThat(MbWayDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
