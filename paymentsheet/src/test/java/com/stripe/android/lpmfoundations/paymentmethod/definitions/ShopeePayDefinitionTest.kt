package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class ShopeePayDefinitionTest {
    @Test
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.ShopeePay),
        )

        assertThat(ShopeePayDefinition.isSupported(metadata)).isEqualTo(
            intentScenario == LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
        assertThat(ShopeePayDefinition.requiresMandate(metadata)).isEqualTo(
            false
        )
    }

    @Test
    fun `form has no method-specific copy`() {
        val intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.ShopeePay),
        )
        val formElements = ShopeePayDefinition.formElements(metadata)

        assertThat(formElements).isEmpty()
    }

    @Test
    fun `terms display never hides terms without changing confirmation requirements`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.ShopeePay),
            termsDisplay = mapOf(PaymentMethod.Type.ShopeePay to PaymentSheet.TermsDisplay.NEVER),
        )
        val formElements = ShopeePayDefinition.formElements(metadata)

        assertThat(formElements).isEmpty()
        assertThat(ShopeePayDefinition.requiresMandate(metadata)).isEqualTo(false)
    }

    @Test
    fun `billing address uses all supported countries`() {
        val formElements = ShopeePayDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("shopeepay")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            ),
            initialValues = mapOf(FormFieldId.Country to "CA"),
        )

        val addressElement = (formElements.single() as SectionElement).fields.single() as AddressElement
        assertThat(addressElement.countryElement.controller.displayItems)
            .hasSize(CountryUtils.supportedBillingCountries.size)
        assertThat(addressElement.countryElement.controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `billing countries follow merchant configuration`(
        @TestParameter(value = ["Full", "AutomaticWithTax"])
        billingMode: LpmBillingDetailsCollectionMode,
    ) {
        val metadata = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.ShopeePay,
            billingDetailsCollectionMode = billingMode,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ).metadata().copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = billingMode.billingDetailsCollectionConfiguration().address,
                allowedCountries = setOf("CA"),
            ),
        )
        val formElements = ShopeePayDefinition.formElements(
            metadata = metadata,
            initialValues = mapOf(FormFieldId.Country to "CA"),
        )
        val addressField = formElements.filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .single()
        val countryElement = when (addressField) {
            is AddressElement -> addressField.countryElement
            is BillingAddressElement -> addressField.countryElement
            else -> error("Expected a billing address field")
        }

        assertThat(countryElement.controller.displayItems).containsExactly("🇨🇦 Canada")
        assertThat(countryElement.controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `collects configured contact information`() {
        val formElements = ShopeePayDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("shopeepay")),
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
        assertThat(ShopeePayDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
