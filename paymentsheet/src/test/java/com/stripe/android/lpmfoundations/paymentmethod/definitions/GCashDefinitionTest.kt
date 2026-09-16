package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.SectionElement
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
    fun `form matches web mandate`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.GCash),
        )
        val formElements = GCashDefinition.formElements(metadata)

        val hasSetup = intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        assertThat(formElements).hasSize(if (hasSetup) 1 else 0)
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

        assertThat(formElements).isEmpty()
        assertThat(GCashDefinition.requiresMandate(metadata)).isEqualTo(true)
    }

    @Test
    fun `limits billing country to Philippines`() {
        val formElements = GCashDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("gcash")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            ),
        )

        val addressElement = (formElements.single() as SectionElement).fields.single() as AddressElement
        assertThat(addressElement.countryElement.controller.displayItems).containsExactly("🇵🇭 Philippines")
        assertThat(addressElement.countryElement.controller.rawFieldValue.value).isEqualTo("PH")
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

        assertThat(formElements).hasSize(2)
        checkPhoneField(formElements, 0)
        checkEmailField(formElements, 1)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(GCashDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
