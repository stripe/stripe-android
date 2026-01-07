package com.stripe.android.lpmfoundations.paymentmethod.definitions

import androidx.annotation.StringRes
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
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.MandateTextElement
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

internal fun PaymentMethodDefinition.mandateTest(
    @StringRes mandateRes: Int,
    arguments: (metadata: PaymentMethodMetadata) -> List<String>,
) {
    val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = SetupIntentFactory.create(
            paymentMethodTypes = listOf(type.code)
        ),
    )

    val formElements = formElements(metadata = metadata)

    assertThat(formElements).hasSize(1)
    assertThat(formElements[0]).isInstanceOf<MandateTextElement>()

    val mandateElement = formElements[0] as MandateTextElement

    assertThat(mandateElement.stringResId)
        .isEqualTo(mandateRes)
    assertThat(mandateElement.args)
        .isEqualTo(arguments(metadata))
}

internal fun PaymentMethodDefinition.noMandateWithTermsDisplayNeverTest() {
    val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = SetupIntentFactory.create(
            paymentMethodTypes = listOf(type.code)
        ),
        termsDisplay = mapOf(
            type to PaymentSheet.TermsDisplay.NEVER,
        )
    )

    val formElements = formElements(metadata = metadata)

    assertThat(formElements).isEmpty()
}

internal fun PaymentMethodDefinition.mandateWithContactFieldsTest(
    @StringRes mandateRes: Int,
    arguments: (metadata: PaymentMethodMetadata) -> List<String>,
) {
    val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = SetupIntentFactory.create(
            paymentMethodTypes = listOf(type.code)
        ),
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
        ),
    )

    val formElements = formElements(metadata = metadata)

    assertThat(formElements).hasSize(3)

    checkPhoneField(formElements, 0)
    checkEmailField(formElements, 1)

    assertThat(formElements[2]).isInstanceOf<MandateTextElement>()

    val mandateElement = formElements[2] as MandateTextElement

    assertThat(mandateElement.stringResId)
        .isEqualTo(mandateRes)
    assertThat(mandateElement.args)
        .isEqualTo(arguments(metadata))
}

internal fun PaymentMethodDefinition.mandateWithBillingInformationTest(
    @StringRes mandateRes: Int,
    arguments: (metadata: PaymentMethodMetadata) -> List<String>,
) {
    val metadata = PaymentMethodMetadataFactory.create(
        stripeIntent = SetupIntentFactory.create(
            paymentMethodTypes = listOf(type.code)
        ),
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
        )
    )

    val formElements = formElements(metadata = metadata)

    assertThat(formElements).hasSize(5)

    checkNameField(formElements, 0)
    checkPhoneField(formElements, 1)
    checkEmailField(formElements, 2)
    checkBillingField(formElements, 3)

    assertThat(formElements[4]).isInstanceOf<MandateTextElement>()

    val mandateElement = formElements[4] as MandateTextElement

    assertThat(mandateElement.stringResId)
        .isEqualTo(mandateRes)
    assertThat(mandateElement.args)
        .isEqualTo(arguments(metadata))
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
