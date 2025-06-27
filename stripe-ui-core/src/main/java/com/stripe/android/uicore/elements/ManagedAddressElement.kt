package com.stripe.android.uicore.elements

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow

open class ManagedAddressElement(
    override val identifier: IdentifierSpec,
    rawValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        rawValuesMap[IdentifierSpec.Country]
    ),
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<IdentifierSpec, String?>?,
    isPlacesAvailable: IsPlacesAvailable = DefaultIsPlacesAvailable(),
    hideCountry: Boolean = false,
    phoneNumberState: PhoneNumberState = PhoneNumberState.HIDDEN,
    managedAddressManagerFactory: ManagedAddressManager.Factory? = null
) : SectionFieldElement {
    private val controller by lazy {
        ManagedAddressController(
            identifier = identifier,
            rawValuesMap = rawValuesMap,
            countryCodes = countryCodes,
            countryDropdownFieldController = countryDropdownFieldController,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
            isPlacesAvailable = isPlacesAvailable,
            hideCountry = hideCountry,
            phoneNumberState = phoneNumberState,
            managedAddressManagerFactory = managedAddressManagerFactory,
        )
    }

    override val allowsUserInteraction: Boolean = true

    override val mandateText: ResolvableString? = null

    override fun sectionFieldErrorController(): SectionFieldErrorController {
        return controller
    }

    override fun setRawValue(rawValuesMap: Map<IdentifierSpec, String?>) {
        controller.addressElementFlow.value.setRawValue(rawValuesMap)
    }

    override fun getFormFieldValueFlow() = controller.addressElementFlow
        .flatMapLatestAsStateFlow { addressElement ->
            addressElement.getFormFieldValueFlow()
        }

    override fun getTextFieldIdentifiers() = controller.addressElementFlow
        .flatMapLatestAsStateFlow { addressElement ->
            addressElement.getTextFieldIdentifiers()
        }
}
