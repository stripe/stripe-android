package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDefinitionTest {
    @Test
    fun `createFormElements returns minimal set of fields`() {
        val formElements = CardDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                )
            )
        )
        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].identifier.v1).isEqualTo("card_details")
    }

    @Test
    fun `createFormElements returns default set of fields`() {
        val formElements = CardDefinition.formElements(PaymentMethodMetadataFactory.create())
        assertThat(formElements).hasSize(2)
        assertThat(formElements[0].identifier.v1).isEqualTo("card_details")
        assertThat(formElements[1].identifier.v1).isEqualTo("credit_billing_section")
    }

    @Test
    fun `createFormElements returns requested billing details fields`() {
        val formElements = CardDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )
        assertThat(formElements).hasSize(3)
        assertThat(formElements[0].identifier.v1).isEqualTo("billing_details[email]_section")
        assertThat(formElements[1].identifier.v1).isEqualTo("card_details")
        assertThat(formElements[2].identifier.v1).isEqualTo("credit_billing_section")

        val contactElement = formElements[0] as SectionElement
        assertThat(contactElement.fields).hasSize(2)
        assertThat(contactElement.fields[0].identifier.v1).isEqualTo("billing_details[email]")
        assertThat(contactElement.fields[1].identifier.v1).isEqualTo("billing_details[phone]")
    }

    @Test
    fun `createFormElements adds a field for same as shipping`() {
        val formElements = CardDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                shippingDetails = AddressDetails(isCheckboxSelected = true)
            )
        )
        assertThat(formElements).hasSize(2)
        assertThat(formElements[0].identifier.v1).isEqualTo("card_details")
        assertThat(formElements[1].identifier.v1).isEqualTo("credit_billing_section")

        val billingDetailsElement = formElements[1] as SectionElement
        assertThat(billingDetailsElement.fields).hasSize(2)
        assertThat(billingDetailsElement.fields[0].identifier.v1).isEqualTo("credit_billing")
        assertThat(billingDetailsElement.fields[1].identifier.v1).isEqualTo("same_as_shipping")
    }

    @Test
    fun `createFormElements returns save_for_future_use`() {
        val formElements = CardDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                ),
                hasCustomerConfiguration = true,
            )
        )
        assertThat(formElements).hasSize(2)
        assertThat(formElements[0].identifier.v1).isEqualTo("card_details")
        assertThat(formElements[1].identifier.v1).isEqualTo("save_for_future_use")
    }
}
