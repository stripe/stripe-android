package com.stripe.android.lpmfoundations.luxe

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test

class ContactInformationCollectionModeTest {
    @Test
    fun `name collectionMode comes from name in BillingDetailsCollectionConfiguration`() {
        val collectionMode = ContactInformationCollectionMode.Name.collectionMode(
            BillingDetailsCollectionConfiguration(
                name = BillingDetailsCollectionConfiguration.CollectionMode.Always
            )
        )
        assertThat(collectionMode).isEqualTo(BillingDetailsCollectionConfiguration.CollectionMode.Always)
    }

    @Test
    fun `name formElement creates name`() {
        val sectionElement = ContactInformationCollectionMode.Name.formElement(emptyMap()) as SectionElement
        val formElement = sectionElement.fields.first()
        assertThat(formElement.identifier.v1).isEqualTo("billing_details[name]")
    }

    @Test
    fun `phone collectionMode comes from phone in BillingDetailsCollectionConfiguration`() {
        val collectionMode = ContactInformationCollectionMode.Phone.collectionMode(
            BillingDetailsCollectionConfiguration(
                phone = BillingDetailsCollectionConfiguration.CollectionMode.Always
            )
        )
        assertThat(collectionMode).isEqualTo(BillingDetailsCollectionConfiguration.CollectionMode.Always)
    }

    @Test
    fun `phone formElement creates phone`() {
        val sectionElement = ContactInformationCollectionMode.Phone.formElement(emptyMap()) as SectionElement
        val formElement = sectionElement.fields.first()
        assertThat(formElement.identifier.v1).isEqualTo("billing_details[phone]")
    }

    @Test
    fun `email collectionMode comes from email in BillingDetailsCollectionConfiguration`() {
        val collectionMode = ContactInformationCollectionMode.Email.collectionMode(
            BillingDetailsCollectionConfiguration(
                email = BillingDetailsCollectionConfiguration.CollectionMode.Always
            )
        )
        assertThat(collectionMode).isEqualTo(BillingDetailsCollectionConfiguration.CollectionMode.Always)
    }

    @Test
    fun `email formElement creates email`() {
        val sectionElement = ContactInformationCollectionMode.Email.formElement(emptyMap()) as SectionElement
        val formElement = sectionElement.fields.first()
        assertThat(formElement.identifier.v1).isEqualTo("billing_details[email]")
    }

    @Test
    fun `isAllowed functions correctly`() {
        val emailRequiredConfiguration = BillingDetailsCollectionConfiguration(
            email = BillingDetailsCollectionConfiguration.CollectionMode.Always
        )
        val emailAutomaticConfiguration = BillingDetailsCollectionConfiguration(
            email = BillingDetailsCollectionConfiguration.CollectionMode.Automatic
        )
        val emailNeverConfiguration = BillingDetailsCollectionConfiguration(
            email = BillingDetailsCollectionConfiguration.CollectionMode.Never
        )
        assertThat(ContactInformationCollectionMode.Email.isAllowed(emailRequiredConfiguration)).isTrue()
        assertThat(ContactInformationCollectionMode.Email.isAllowed(emailAutomaticConfiguration)).isTrue()
        assertThat(ContactInformationCollectionMode.Email.isAllowed(emailNeverConfiguration)).isFalse()
    }

    @Test
    fun `isRequired functions correctly`() {
        val emailRequiredConfiguration = BillingDetailsCollectionConfiguration(
            email = BillingDetailsCollectionConfiguration.CollectionMode.Always
        )
        val emailAutomaticConfiguration = BillingDetailsCollectionConfiguration(
            email = BillingDetailsCollectionConfiguration.CollectionMode.Automatic
        )
        val emailNeverConfiguration = BillingDetailsCollectionConfiguration(
            email = BillingDetailsCollectionConfiguration.CollectionMode.Never
        )
        assertThat(ContactInformationCollectionMode.Email.isRequired(emailRequiredConfiguration)).isTrue()
        assertThat(ContactInformationCollectionMode.Email.isRequired(emailAutomaticConfiguration)).isFalse()
        assertThat(ContactInformationCollectionMode.Email.isRequired(emailNeverConfiguration)).isFalse()
    }
}
