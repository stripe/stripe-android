package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test

class KonbiniDefinitionTest {
    private val konbiniMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("konbini"),
        )
    )

    @Test
    fun `createFormElements returns minimal set of fields`() {
        val formElements = KonbiniDefinition.formElements(konbiniMetadata)
        assertThat(formElements).hasSize(3)
        assertThat(formElements[0].identifier.v1).isEqualTo("billing_details[name]_section")
        assertThat(formElements[1].identifier.v1).isEqualTo("billing_details[email]_section")
        assertThat(formElements[2].identifier.v1).isEqualTo("konbini[confirmation_number]_section")
        val confirmationNumberSection = formElements[2] as SectionElement
        assertThat(confirmationNumberSection.fields[0].identifier.v1).isEqualTo("konbini[confirmation_number]")
    }

    @Test
    fun `createFormElements returns billing address collection fields`() {
        val formElements = KonbiniDefinition.formElements(
            metadata = konbiniMetadata.copy(
                billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                    name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )
        assertThat(formElements).hasSize(5)
        assertThat(formElements[0].identifier.v1).isEqualTo("billing_details[name]_section")
        assertThat(formElements[1].identifier.v1).isEqualTo("billing_details[phone]_section")
        assertThat(formElements[2].identifier.v1).isEqualTo("billing_details[email]_section")
        assertThat(formElements[3].identifier.v1).isEqualTo("konbini[confirmation_number]_section")
        assertThat(formElements[4].identifier.v1).isEqualTo("billing_details[address]_section")
    }

    @Test
    fun `createFormElements omits billing address collection fields when specified as never`() {
        val formElements = KonbiniDefinition.formElements(
            metadata = konbiniMetadata.copy(
                billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                    name = BillingDetailsCollectionConfiguration.CollectionMode.Never,
                    email = BillingDetailsCollectionConfiguration.CollectionMode.Never,
                )
            )
        )
        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].identifier.v1).isEqualTo("konbini[confirmation_number]_section")
    }
}
