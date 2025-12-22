package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.SectionElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KlarnaDefinitionTest {
    @Test
    fun `createFormElements returns header, email, and country elements for PaymentIntent`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("klarna")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(3)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkCountryField(formElements, 2)
    }

    @Test
    fun `createFormElements returns header, email, country, and mandate elements for SetupIntent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("klarna")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
        )

        val formElements = KlarnaDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(4)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkCountryField(formElements, 2)
        checkMandateField(formElements, metadata, 3)
    }

    @Test
    fun `createFormElements does not return mandate when termsDisplay is NEVER`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("klarna")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            ),
            termsDisplay = mapOf(
                PaymentMethod.Type.Klarna to PaymentSheet.TermsDisplay.NEVER
            )
        )

        val formElements = KlarnaDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(3)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkCountryField(formElements, 2)
    }

    @Test
    fun `createFormElements includes name when requested`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("klarna")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(4)

        checkKlarnaHeaderText(formElements, 0)
        checkNameField(formElements, 1)
        checkEmailField(formElements, 2)
        checkCountryField(formElements, 3)
    }

    @Test
    fun `createFormElements includes phone when requested`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("klarna")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(4)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkCountryField(formElements, 3)
    }

    @Test
    fun `createFormElements includes address when full address collection is enabled`() {
        val formElements = KlarnaDefinition.formElements(
            metadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(
                    paymentMethodTypes = listOf("klarna")
                ),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )

        assertThat(formElements).hasSize(4)

        checkKlarnaHeaderText(formElements, 0)
        checkEmailField(formElements, 1)
        checkCountryField(formElements, 2)
        checkBillingField(formElements, 3)
    }

    @Test
    fun `createFormElements with all billing fields and mandate for SetupIntent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create(
                paymentMethodTypes = listOf("klarna")
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        )

        val formElements = KlarnaDefinition.formElements(metadata = metadata)

        assertThat(formElements).hasSize(7)

        checkKlarnaHeaderText(formElements, 0)
        checkNameField(formElements, 1)
        checkEmailField(formElements, 2)
        checkPhoneField(formElements, 3)
        checkCountryField(formElements, 4)
        checkBillingField(formElements, 5)
        checkMandateField(formElements, metadata, 6)
    }

    private fun checkKlarnaHeaderText(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val element = formElements[position]

        assertThat(element).isInstanceOf<StaticTextElement>()
        assertThat(element.identifier.v1).isEqualTo("klarna_header_text")

        val textElement = element as StaticTextElement

        assertThat(textElement.text).isEqualTo(R.string.stripe_klarna_buy_now_pay_later.resolvableString)
    }

    private fun checkCountryField(
        formElements: List<FormElement>,
        position: Int,
    ) {
        val element = formElements[position]

        assertThat(element.identifier.v1).isEqualTo("billing_details[address][country]_section")
        assertThat(element).isInstanceOf<SectionElement>()

        val countrySection = element as SectionElement

        assertThat(countrySection.fields).hasSize(1)
        assertThat(countrySection.fields[0]).isInstanceOf<CountryElement>()
    }

    private fun checkMandateField(
        formElements: List<FormElement>,
        metadata: PaymentMethodMetadata,
        position: Int,
    ) {
        val element = formElements[position]

        assertThat(element).isInstanceOf<MandateTextElement>()

        val mandateElement = element as MandateTextElement

        assertThat(mandateElement.stringResId).isEqualTo(R.string.stripe_klarna_mandate)
        assertThat(mandateElement.args).isEqualTo(listOf(metadata.merchantName, metadata.merchantName))
    }
}
