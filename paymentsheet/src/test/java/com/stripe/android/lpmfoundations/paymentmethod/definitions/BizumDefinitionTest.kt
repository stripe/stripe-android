package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

class BizumDefinitionTest {
    private val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFactory.create(
            paymentMethodTypes = listOf("bizum"),
        )
    )

    @Test
    fun `createFormElements requires phone number`() {
        val formElements = BizumDefinition.formElements(metadata)

        assertThat(formElements).hasSize(1)
        checkPhoneField(formElements, 0)
    }

    @Test
    fun `requirements include unsupported for setup`() {
        assertThat(BizumDefinition.requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup = false))
            .containsExactly(AddPaymentMethodRequirement.UnsupportedForSetup)
    }

    @Test
    fun `requiresMandate returns false`() {
        assertThat(BizumDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `supportedAsSavedPaymentMethod is false`() {
        assertThat(BizumDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
