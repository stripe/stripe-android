package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlikDefinitionTest {
    private val blikMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("blik"),
        )
    )

    @Test
    fun `createFormElements returns minimal set of fields`() {
        val formElements = BlikDefinition.formElements(blikMetadata)
        assertThat(formElements).hasSize(1)
        val sectionElement = formElements[0] as SectionElement
        assertThat(sectionElement.fields).hasSize(1)
        assertThat(sectionElement.fields[0].identifier.v1).isEqualTo("blik[code]")
    }

    @Test
    fun `createFormElements returns billing address collection fields`() {
        val formElements = BlikDefinition.formElements(
            metadata = blikMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )
        assertThat(formElements).hasSize(5)
        assertThat(formElements[0].identifier.v1).isEqualTo("billing_details[name]_section")
        assertThat(formElements[1].identifier.v1).isEqualTo("billing_details[phone]_section")
        assertThat(formElements[2].identifier.v1).isEqualTo("billing_details[email]_section")
        assertThat(formElements[3].identifier.v1).isEqualTo("blik[code]_section")
        assertThat(formElements[4].identifier.v1).isEqualTo("billing_details[address]_section")
    }
}
