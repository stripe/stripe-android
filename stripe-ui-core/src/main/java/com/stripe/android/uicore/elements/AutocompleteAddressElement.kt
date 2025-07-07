package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AutocompleteAddressElement(
    override val identifier: IdentifierSpec,
    initialValues: Map<IdentifierSpec, String?>,
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        initialValues[IdentifierSpec.Country]
    ),
    phoneNumberState: PhoneNumberState = PhoneNumberState.HIDDEN,
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<IdentifierSpec, String?>?,
    interactorFactory: AutocompleteAddressInteractor.Factory,
    hideCountry: Boolean = false,
    hideName: Boolean = true,
) : AddressFieldsElement {
    private val controller by lazy {
        AutocompleteAddressController(
            identifier = identifier,
            initialValues = initialValues,
            countryCodes = countryCodes,
            countryDropdownFieldController = countryDropdownFieldController,
            phoneNumberState = phoneNumberState,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
            interactorFactory = interactorFactory,
            hideCountry = hideCountry,
            hideName = hideName,
        )
    }

    override val countryElement: CountryElement = controller.countryElement

    override val allowsUserInteraction: Boolean = true

    override val mandateText: ResolvableString? = null

    override fun sectionFieldErrorController() = controller

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        controller.addressElementFlow.value.setRawValue(rawValuesMap)
    }

    override fun getFormFieldValueFlow() = controller.formFieldValues

    override fun getTextFieldIdentifiers() = controller.textFieldIdentifiers
}
