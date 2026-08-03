package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.AddressElement
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
            )
        )

        assertThat(formElements).hasSize(1)

        checkCountryField(formElements, 0)
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
                )
            )
        )

        assertThat(formElements).hasSize(4)

        checkCountryField(formElements, 0)
        checkNameField(formElements, 1)
        checkEmailField(formElements, 2)
        checkPhoneField(formElements, 3)
    }

    @Test
    fun `createFormElements returns country and billing address when full address collection enabled`() {
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
                )
            )
        )

        assertThat(formElements).hasSize(4)

        checkFullAddressField(formElements, 0)
        checkNameField(formElements, 1)
        checkEmailField(formElements, 2)
        checkPhoneField(formElements, 3)
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
    ) {
        val element = formElements[position]
        assertThat(element).isInstanceOf(SectionElement::class.java)

        val section = element as SectionElement
        assertThat(section.fields).hasSize(1)
        val addressElement = section.fields[0] as BillingAddressElement
        assertThat(addressElement.countryElement.controller.displayItems).hasSize(3)
        assertThat(addressElement.hiddenIdentifiers.value).containsAtLeast(
            IdentifierSpec.Line1,
            IdentifierSpec.City,
            IdentifierSpec.State,
            IdentifierSpec.PostalCode,
        )
    }

    private fun checkFullAddressField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val section = formElements[position] as SectionElement
        assertThat(section.identifier.v1).isEqualTo("billing_details[address]_section")
        val addressElement = section.fields.single() as BillingAddressElement
        assertThat(addressElement.addressElement).isInstanceOf(AddressElement::class.java)
        assertThat(addressElement.hiddenIdentifiers.value).isEmpty()
    }
}
