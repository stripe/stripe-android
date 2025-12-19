package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FpxDefinitionTest {
    private val fpxMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFactory.create(
            paymentMethodTypes = listOf("fpx"),
        )
    )

    @Test
    fun `createFormElements returns bank dropdown`() {
        val formElements = FpxDefinition.formElements(fpxMetadata)

        assertThat(formElements).hasSize(1)

        checkFpxDropdownField(formElements, 0)
    }

    @Test
    fun `createFormElements returns billing address collection fields`() {
        val formElements = FpxDefinition.formElements(
            metadata = fpxMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )

        assertThat(formElements).hasSize(5)

        checkNameField(formElements, 0)
        checkPhoneField(formElements, 1)
        checkEmailField(formElements, 2)
        checkFpxDropdownField(formElements, 3)
        checkBillingField(formElements, 4)
    }

    @Test
    fun `createFormElements returns contact information fields`() {
        val formElements = FpxDefinition.formElements(
            metadata = fpxMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(3)

        checkPhoneField(formElements, 0)
        checkEmailField(formElements, 1)
        checkFpxDropdownField(formElements, 2)
    }

    private fun checkFpxDropdownField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val bankSection = formElements[position] as SectionElement
        assertThat(bankSection.fields).hasSize(1)
        val dropdownElement = bankSection.fields[0] as SimpleDropdownElement
        assertThat(dropdownElement.identifier.v1).isEqualTo("fpx[bank]")
    }
}
