package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.uicore.elements.FormElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdealWeroDefinitionTest {
    @Test
    fun `createFormElements returns name element for PaymentIntent`() {
        val formElements = IdealWeroDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("ideal")
                )
            )
        )

        assertThat(formElements).hasSize(1)

        checkNameField(formElements, 0)
    }

    @Test
    fun `createFormElements returns name, email, and mandate for SetupIntent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("ideal")
            )
        )

        val formElements = IdealWeroDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(3)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkMandateField(formElements, metadata, 2)
    }

    @Test
    fun `createFormElements returns no mandate when termsDisplay is NEVER for SetupIntent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("ideal")
            ),
            termsDisplay = mapOf(
                PaymentMethod.Type.Ideal to PaymentSheet.TermsDisplay.NEVER
            )
        )

        val formElements = IdealWeroDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(2)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
    }

    @Test
    fun `createFormElements includes phone when requested for PaymentIntent`() {
        val formElements = IdealWeroDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("ideal")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                )
            )
        )

        assertThat(formElements).hasSize(2)

        checkNameField(formElements, 0)
        checkPhoneField(formElements, 1)
    }

    @Test
    fun `createFormElements includes contact information fields for PaymentIntent`() {
        val formElements = IdealWeroDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("ideal")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(3)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
    }

    @Test
    fun `createFormElements includes all billing details for PaymentIntent`() {
        val formElements = IdealWeroDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("ideal")
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

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkBillingField(formElements, 3)
    }

    @Test
    fun `createFormElements returns name, email, phone, address and mandate for SetupIntent with all billing`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("ideal")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        )

        val formElements = IdealWeroDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(5)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkBillingField(formElements, 3)
        checkMandateField(formElements, metadata, 4)
    }

    @Test
    fun `createFormElements returns contact fields and mandate for SetupIntent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("ideal")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
        )

        val formElements = IdealWeroDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(4)

        checkNameField(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkMandateField(formElements, metadata, 3)
    }

    private fun checkMandateField(
        formElements: List<FormElement>,
        metadata: PaymentMethodMetadata,
        position: Int,
    ) {
        val element = formElements[position]

        assertThat(element).isInstanceOf(MandateTextElement::class.java)

        val mandateElement = element as MandateTextElement

        assertThat(mandateElement.stringResId).isEqualTo(R.string.stripe_sepa_mandate)
        assertThat(mandateElement.args).isEqualTo(listOf(metadata.merchantName))
    }
}
