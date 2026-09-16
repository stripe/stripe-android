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
internal class GoPayDefinitionTest {
    @Test
    fun `supports payment and setup without an inline mandate`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.GoPay),
        )

        assertThat(GoPayDefinition.isSupported(metadata)).isTrue()
        val instructions = GoPayDefinition.formElements(metadata).single() as StaticTextElement
        assertThat(instructions.text).isEqualTo(R.string.stripe_paymentsheet_redirect_instructions.resolvableString)
        assertThat(GoPayDefinition.requiresMandate(metadata)).isEqualTo(
            intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
    }

    @Test
    fun `setup confirmation still needs mandate data when terms display is never`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent
                .stripeIntent(PaymentMethod.Type.GoPay),
            termsDisplay = mapOf(PaymentMethod.Type.GoPay to PaymentSheet.TermsDisplay.NEVER),
        )

        assertThat(GoPayDefinition.requiresMandate(metadata)).isTrue()
        assertThat(GoPayDefinition.formElements(metadata).single()).isInstanceOf(StaticTextElement::class.java)
    }

    @Test
    fun `collects configured contact information`() {
        val formElements = GoPayDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("gopay")),
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
    fun `collects configured billing address`() {
        val formElements = GoPayDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("gopay")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            )
        )

        assertThat(formElements).hasSize(2)
        checkBillingField(formElements, 0)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(GoPayDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
