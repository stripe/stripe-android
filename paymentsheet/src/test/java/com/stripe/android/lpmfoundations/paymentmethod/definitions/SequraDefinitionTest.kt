package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SequraDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        SequraDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        SequraDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        SequraDefinition.basicFormWithBillingInformationTest()
    }

    @Test
    fun `requirements include unsupported for setup`() {
        assertThat(
            SequraDefinition.requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup = false)
        ).containsExactly(AddPaymentMethodRequirement.UnsupportedForSetup)
    }

    @Test
    fun `requiresMandate returns false`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("sequra")
            )
        )

        assertThat(SequraDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `supportedAsSavedPaymentMethod is false`() {
        assertThat(SequraDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
