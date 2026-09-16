package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class QrisDefinitionTest {
    @Test
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.Qris),
        )

        assertThat(QrisDefinition.isSupported(metadata)).isEqualTo(
            intentScenario == LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
        assertThat(QrisDefinition.requiresMandate(metadata)).isEqualTo(
            false
        )
    }

    @Test
    fun `form has no method-specific copy`() {
        val intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.Qris),
        )
        val formElements = QrisDefinition.formElements(metadata)

        assertThat(formElements).isEmpty()
    }

    @Test
    fun `terms display never hides terms without changing confirmation requirements`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.Qris),
            termsDisplay = mapOf(PaymentMethod.Type.Qris to PaymentSheet.TermsDisplay.NEVER),
        )
        val formElements = QrisDefinition.formElements(metadata)

        assertThat(formElements).isEmpty()
        assertThat(QrisDefinition.requiresMandate(metadata)).isEqualTo(false)
    }

    @Test
    fun `limits billing countries and defaults to United States`() {
        val formElements = QrisDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("qris")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            ),
        )

        val addressElement = (formElements.single() as SectionElement).fields.single() as AddressElement
        assertThat(addressElement.countryElement.controller.displayItems).containsExactly(
            "🇺🇸 United States",
            "🇮🇩 Indonesia",
        ).inOrder()
        assertThat(addressElement.countryElement.controller.rawFieldValue.value).isEqualTo("US")
    }

    @Test
    fun `collects configured contact information`() {
        val formElements = QrisDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("qris")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                ),
            )
        )

        assertThat(formElements).hasSize(2)
        checkPhoneField(formElements, 0)
        checkEmailField(formElements, 1)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(QrisDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
