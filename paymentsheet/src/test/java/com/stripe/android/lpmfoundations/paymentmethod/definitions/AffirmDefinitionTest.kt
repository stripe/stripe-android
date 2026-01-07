package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.elements.AffirmHeaderElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AffirmDefinitionTest {
    @Test
    fun `createFormElements returns just header in its elements`() {
        val formElements = AffirmDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("affirm")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(1)
        assertThat(formElements[0].identifier.v1).isEqualTo("affirm_header")
        assertThat(formElements[0]).isInstanceOf<AffirmHeaderElement>()
    }

    @Test
    fun `createFormElements returns header & requested contact information fields`() {
        val formElements = AffirmDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("affirm")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(3)

        assertThat(formElements[0].identifier.v1).isEqualTo("affirm_header")
        assertThat(formElements[0]).isInstanceOf<AffirmHeaderElement>()

        checkPhoneField(formElements, 1)
        checkEmailField(formElements, 2)
    }

    @Test
    fun `createFormElements returns header & all billing details fields`() {
        val formElements = AffirmDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("affirm")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )

        assertThat(formElements).hasSize(5)

        assertThat(formElements[0].identifier.v1).isEqualTo("affirm_header")
        assertThat(formElements[0]).isInstanceOf<AffirmHeaderElement>()

        checkNameField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkEmailField(formElements, 3)
        checkBillingField(formElements, 4)
    }
}
