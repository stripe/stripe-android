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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
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
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.MoMo),
        )

        assertThat(MoMoDefinition.isSupported(metadata)).isEqualTo(
            true
        )
        assertThat(MoMoDefinition.requiresMandate(metadata)).isEqualTo(
            intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
    }

    @Test
    fun `form matches web instructions and mandate`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.MoMo),
        )
        val formElements = MoMoDefinition.formElements(metadata)

        val instructions = formElements.first() as StaticTextElement
        assertThat(instructions.text).isEqualTo(R.string.stripe_paymentsheet_redirect_instructions.resolvableString)
        assertThat(formElements).hasSize(1)
    }

    @Test
    fun `terms display never hides terms without changing confirmation requirements`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent
                .stripeIntent(PaymentMethod.Type.MoMo),
            termsDisplay = mapOf(PaymentMethod.Type.MoMo to PaymentSheet.TermsDisplay.NEVER),
        )
        val formElements = MoMoDefinition.formElements(metadata)

        assertThat(formElements.single()).isInstanceOf(StaticTextElement::class.java)
        assertThat(MoMoDefinition.requiresMandate(metadata)).isEqualTo(true)
    }

    @Test
    fun `collects configured contact information`() {
        val formElements = MoMoDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("momo")),
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
        assertThat(MoMoDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
