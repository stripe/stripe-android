package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class MonduDefinitionTest {
    @Test
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.Mondu),
        )

        assertThat(MonduDefinition.isSupported(metadata)).isEqualTo(
            intentScenario == LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
        assertThat(MonduDefinition.requiresMandate(metadata)).isEqualTo(
            false
        )
    }

    @Test
    fun `form matches web buyer message`() {
        val intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.Mondu),
        )
        val formElements = MonduDefinition.formElements(metadata)

        val message = formElements.first() as StaticTextElement
        assertThat(message.text).isEqualTo(R.string.stripe_mondu_buyer_message.resolvableString)
        assertThat(formElements).hasSize(1)
    }

    @Test
    fun `limits billing countries and defaults to Germany`() {
        val formElements = MonduDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("mondu")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            ),
        )

        val addressElement = (formElements.first() as SectionElement).fields.single() as AddressElement
        assertThat(addressElement.countryElement.controller.displayItems).hasSize(14)
        assertThat(addressElement.countryElement.controller.rawFieldValue.value).isEqualTo("DE")
    }

    @Test
    fun `terms display never hides terms without changing confirmation requirements`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.Mondu),
            termsDisplay = mapOf(PaymentMethod.Type.Mondu to PaymentSheet.TermsDisplay.NEVER),
        )
        val formElements = MonduDefinition.formElements(metadata)

        assertThat(formElements.single()).isInstanceOf(StaticTextElement::class.java)
        assertThat(MonduDefinition.requiresMandate(metadata)).isEqualTo(false)
    }

    @Test
    fun `collects configured contact information`() {
        val formElements = MonduDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("mondu")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                ),
            )
        )

        assertThat(formElements).hasSize(3)
        checkPhoneField(formElements, 0)
        checkEmailField(formElements, 1)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(MonduDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
