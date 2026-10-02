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
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.StaticTextElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class GCashDefinitionTest {
    @Test
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.GCash),
        )

        assertThat(GCashDefinition.isSupported(metadata)).isEqualTo(
            true
        )
        assertThat(GCashDefinition.requiresMandate(metadata)).isEqualTo(
            intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
    }

    @Test
    fun `form matches web instructions and mandate`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.GCash),
        )
        val formElements = GCashDefinition.formElements(metadata)

        val instructions = formElements.first() as StaticTextElement
        assertThat(instructions.text).isEqualTo(R.string.stripe_paymentsheet_redirect_instructions.resolvableString)
        val hasSetup = intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        assertThat(formElements).hasSize(if (hasSetup) 2 else 1)
        if (hasSetup) {
            val mandate = formElements.last() as MandateTextElement
            assertThat(mandate.stringResId).isEqualTo(R.string.stripe_gcash_mandate)
            assertThat(mandate.args).containsExactly(metadata.merchantName)
        }
    }

    @Test
    fun `terms display never hides terms without changing confirmation requirements`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent
                .stripeIntent(PaymentMethod.Type.GCash),
            termsDisplay = mapOf(PaymentMethod.Type.GCash to PaymentSheet.TermsDisplay.NEVER),
        )
        val formElements = GCashDefinition.formElements(metadata)

        assertThat(formElements.single()).isInstanceOf(StaticTextElement::class.java)
        assertThat(GCashDefinition.requiresMandate(metadata)).isEqualTo(true)
    }

    @Test
    fun `collects configured contact information`() {
        val formElements = GCashDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("gcash")),
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
        assertThat(GCashDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
