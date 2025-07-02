package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class AutocompleteAddressElement(
    override val identifier: IdentifierSpec,
    initialValues: Map<IdentifierSpec, String?>,
    countryCodes: Set<String> = emptySet(),
    phoneNumberState: PhoneNumberState = PhoneNumberState.HIDDEN,
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<IdentifierSpec, String?>?,
    isPlacesAvailable: IsPlacesAvailable = DefaultIsPlacesAvailable(),
    interactor: AutocompleteAddressInteractor,
    hideCountry: Boolean = false,
    hideName: Boolean = true,
) : SectionFieldElement {
    private val controller by lazy {
        AutocompleteAddressController(
            identifier = identifier,
            initialValues = initialValues,
            countryCodes = countryCodes,
            phoneNumberState = phoneNumberState,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
            isPlacesAvailable = isPlacesAvailable,
            interactor = interactor,
            hideCountry = hideCountry,
            hideName = hideName,
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

    override fun getFormFieldValueFlow() = controller.formFieldValues

    override fun getTextFieldIdentifiers() = controller.textFieldIdentifiers
}
