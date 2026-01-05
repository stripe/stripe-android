package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.SimpleDropdownElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpsDefinitionTest {
    private val epsMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("eps"),
        )
    )

    @Test
    fun `createFormElements returns minimal set of fields with name and bank dropdown`() {
        val formElements = EpsDefinition.formElements(epsMetadata)

        assertThat(formElements).hasSize(2)

        checkNameField(formElements, 0)
        checkEpsDropdownField(formElements, 1)
    }

    @Test
    fun `createFormElements returns billing address collection fields`() {
        val formElements = EpsDefinition.formElements(
            metadata = epsMetadata.copy(
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
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkEpsDropdownField(formElements, 3)
        checkBillingField(formElements, 4)
    }

    @Test
    fun `createFormElements returns contact information fields`() {
        val formElements = EpsDefinition.formElements(
            metadata = epsMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(4)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkEpsDropdownField(formElements, 3)
    }

    private fun checkEpsDropdownField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val bankSection = formElements[position] as SectionElement
        assertThat(bankSection.fields).hasSize(1)
        val dropdownElement = bankSection.fields[0] as SimpleDropdownElement
        assertThat(dropdownElement.identifier.v1).isEqualTo("eps[bank]")
    }
}
