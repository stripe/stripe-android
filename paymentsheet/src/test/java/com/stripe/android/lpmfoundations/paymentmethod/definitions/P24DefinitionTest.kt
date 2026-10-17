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
class P24DefinitionTest {
    private val p24Metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("p24"),
        )
    )

    @Test
    fun `createFormElements returns minimal set of fields with name, email, and bank dropdown`() {
        val formElements = P24Definition.formElements(p24Metadata)

        assertThat(formElements).hasSize(3)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkP24DropdownField(formElements, 2)
    }

    @Test
    fun `createFormElements returns billing address collection fields`() {
        val formElements = P24Definition.formElements(
            metadata = p24Metadata.copy(
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
        checkP24DropdownField(formElements, 3)
        checkBillingField(formElements, 4)
    }

    private fun checkP24DropdownField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val bankSection = formElements[position] as SectionElement
        assertThat(bankSection.fields).hasSize(1)
        val dropdownElement = bankSection.fields[0] as SimpleDropdownElement
        assertThat(dropdownElement.identifier.v1).isEqualTo("p24[bank]")
    }
}
