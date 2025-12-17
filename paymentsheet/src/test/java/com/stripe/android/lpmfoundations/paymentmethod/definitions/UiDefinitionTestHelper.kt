package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.compose.runtime.Composable
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodDefinition
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.EmailElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement

@Composable
internal fun PaymentMethodDefinition.CreateFormUi(
    metadata: PaymentMethodMetadata,
    paymentMethodCreateParams: PaymentMethodCreateParams? = null,
    paymentMethodExtraParams: PaymentMethodExtraParams? = null,
    initialLinkUserInput: UserInput? = null,
    linkConfigurationCoordinator: LinkConfigurationCoordinator? = null,
    autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
    tapToAddHelper: TapToAddHelper? = null,
    isValidating: Boolean = false,
) {
    val formElements = formElements(
        metadata = metadata,
        paymentMethodCreateParams = paymentMethodCreateParams,
        paymentMethodExtraParams = paymentMethodExtraParams,
        initialLinkUserInput = initialLinkUserInput,
        linkConfigurationCoordinator = linkConfigurationCoordinator,
        autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        tapToAddHelper = tapToAddHelper,
    ).onEach { element ->
        element.onValidationStateChanged(isValidating)
    }
    FormUI(
        hiddenIdentifiers = emptySet(),
        enabled = true,
        elements = formElements,
        lastTextFieldIdentifier = null,
    )
}

internal fun PaymentMethodDefinition.basicEmptyFormTest() {
    val formElements = formElements(
        metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf(type.code)
            )
        )
    )

    assertThat(formElements).isEmpty()
}

internal fun PaymentMethodDefinition.basicFormWithContactFieldsTest() {
    val formElements = formElements(
        metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf(type.code)
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
        )
    )

    assertThat(formElements).hasSize(2)

    checkPhoneField(formElements, 0)
    checkEmailField(formElements, 1)
}

internal fun PaymentMethodDefinition.basicFormWithBillingInformationTest() {
    val formElements = formElements(
        metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                paymentMethodTypes = listOf(type.code)
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        )
    )

    assertThat(formElements).hasSize(4)

    checkNameField(formElements, 0)
    checkPhoneField(formElements, 1)
    checkEmailField(formElements, 2)
    checkBillingField(formElements, 3)
}

internal fun checkNameField(
    formElements: List<FormElement>,
    position: Int,
) {
    val nameSection = checkSectionField(formElements, "billing_details[name]_section", position)
    assertThat(nameSection.fields).hasSize(1)
    assertThat(nameSection.fields[0]).isInstanceOf<SimpleTextElement>()
}

internal fun checkPhoneField(
    formElements: List<FormElement>,
    position: Int,
) {
    val phoneSection = checkSectionField(formElements, "billing_details[phone]_section", position)
    assertThat(phoneSection.fields).hasSize(1)
    assertThat(phoneSection.fields[0]).isInstanceOf<PhoneNumberElement>()
}

internal fun checkEmailField(
    formElements: List<FormElement>,
    position: Int,
) {
    val emailSection = checkSectionField(formElements, "billing_details[email]_section", position)
    assertThat(emailSection.fields).hasSize(1)
    assertThat(emailSection.fields[0]).isInstanceOf<EmailElement>()
}

internal fun checkBillingField(
    formElements: List<FormElement>,
    position: Int,
) {
    val billingSection = checkSectionField(formElements, "billing_details[address]_section", position)
    assertThat(billingSection.fields).hasSize(1)
    assertThat(billingSection.fields[0]).isInstanceOf<AddressElement>()
}

private fun checkSectionField(
    formElements: List<FormElement>,
    sectionName: String,
    position: Int,
): SectionElement {
    assertThat(formElements[position].identifier.v1)
        .isEqualTo(sectionName)
    assertThat(formElements[position]).isInstanceOf<SectionElement>()

    return formElements[position] as SectionElement
}
