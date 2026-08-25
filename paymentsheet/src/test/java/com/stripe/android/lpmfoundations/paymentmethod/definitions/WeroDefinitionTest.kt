package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.luxe.countryElements
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeroDefinitionTest {
    @Test
    fun `createFormElements returns country element for PaymentIntent`() {
        val formElements = WeroDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("wero")
                )
            ),
            initialValues = mapOf(IdentifierSpec.Country to "BE"),
        )

        assertThat(formElements).hasSize(1)

        val countryElement = checkCountryField(formElements, 0)
        assertRestrictedCountryController(countryElement, initialCountry = "BE")
    }

    @Test
    fun `createFormElements returns country and contact info when requested`() {
        val formElements = WeroDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("wero")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                ),
            )
        )

        assertThat(formElements).hasSize(4)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkCountryField(formElements, 3)
    }

    @Test
    fun `createFormElements returns requested contacts and one address-owned country for full collection`() {
        val formElements = WeroDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("wero")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            ),
            initialValues = mapOf(IdentifierSpec.Country to "DE"),
        )

        assertThat(formElements).hasSize(4)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkBillingField(formElements, 3)

        val addressElement = (formElements[3] as SectionElement).fields.single() as AddressElement
        assertThat(formElements.countryElements()).containsExactly(addressElement.countryElement)
        assertRestrictedCountryController(addressElement.countryElement, initialCountry = "DE")
    }

    @Test
    fun `requiresMandate returns false`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf("wero")
            )
        )

        assertThat(WeroDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `supportedAsSavedPaymentMethod is false`() {
        assertThat(WeroDefinition.supportedAsSavedPaymentMethod).isFalse()
    }

    private fun checkCountryField(
        formElements: List<FormElement>,
        position: Int,
    ): CountryElement {
        val section = formElements[position] as SectionElement
        assertThat(section.fields).hasSize(1)
        return section.fields.single() as CountryElement
    }

    private fun assertRestrictedCountryController(
        countryElement: CountryElement,
        initialCountry: String,
    ) {
        assertThat(countryElement.controller.displayItems).containsExactly(
            "🇧🇪 Belgium",
            "🇫🇷 France",
            "🇩🇪 Germany",
        ).inOrder()
        assertThat(countryElement.controller.rawFieldValue.value).isEqualTo(initialCountry)
    }
}
