package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AutocompleteAddressElement(
    override val identifier: IdentifierSpec,
    initialValues: Map<IdentifierSpec, String?>,
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        initialValues[IdentifierSpec.Country]
    ),
    phoneNumberConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
    nameConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
    emailConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<IdentifierSpec, String?>?,
    interactorFactory: AutocompleteAddressInteractor.Factory,
    hideCountry: Boolean = false,
) : AddressFieldsElement {
    private val controller by lazy {
        AutocompleteAddressController(
            identifier = identifier,
            initialValues = initialValues,
            countryCodes = countryCodes,
            countryDropdownFieldController = countryDropdownFieldController,
            phoneNumberConfig = phoneNumberConfig,
            nameConfig = nameConfig,
            emailConfig = emailConfig,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
            interactorFactory = interactorFactory,
            hideCountry = hideCountry,
        )
    }

    override val addressController: StateFlow<AddressController> = controller.addressController

    override val countryElement: CountryElement = controller.countryElement

    override val allowsUserInteraction: Boolean = true

    override val mandateText: ResolvableString? = null

    override fun sectionFieldErrorController() = controller

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        controller.addressElementFlow.value.setRawValue(rawValuesMap)
    }

    override fun getFormFieldValueFlow() = controller.formFieldValues

    override fun getTextFieldIdentifiers() = controller.textFieldIdentifiers

    override fun onValidationStateChanged(isValidating: Boolean) {
        controller.onValidationStateChanged(isValidating)
    }
}
