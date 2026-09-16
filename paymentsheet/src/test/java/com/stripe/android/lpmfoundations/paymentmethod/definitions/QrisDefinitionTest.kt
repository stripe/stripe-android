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
    fun `form matches web instructions and mandate`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.Qris),
        )
        val formElements = QrisDefinition.formElements(metadata)

        val instructions = formElements.first() as StaticTextElement
        assertThat(instructions.text).isEqualTo(R.string.stripe_paymentsheet_redirect_instructions.resolvableString)
        assertThat(formElements).hasSize(1)
    }

    @Test
    fun `terms display never hides terms without changing confirmation requirements`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.Qris),
            termsDisplay = mapOf(PaymentMethod.Type.Qris to PaymentSheet.TermsDisplay.NEVER),
        )
        val formElements = QrisDefinition.formElements(metadata)

        assertThat(formElements.single()).isInstanceOf(StaticTextElement::class.java)
        assertThat(QrisDefinition.requiresMandate(metadata)).isEqualTo(false)
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

        assertThat(formElements).hasSize(3)
        checkPhoneField(formElements, 0)
        checkEmailField(formElements, 1)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(QrisDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
