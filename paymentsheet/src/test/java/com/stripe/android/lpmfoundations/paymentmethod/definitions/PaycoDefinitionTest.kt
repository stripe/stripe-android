package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaycoDefinitionTest {
    @Test
    fun `createFormElements returns no elements`() {
        PaycoDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        PaycoDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        PaycoDefinition.basicFormWithBillingInformationTest()
    }

    @Test
    fun `requirements include unsupported for setup`() {
        assertThat(
            PaycoDefinition.requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup = false)
        ).containsExactly(AddPaymentMethodRequirement.UnsupportedForSetup)
    }

    @Test
    fun `requiresMandate returns false`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("payco")
            )
        )

        assertThat(PaycoDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `supportedAsSavedPaymentMethod is false`() {
        assertThat(PaycoDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
