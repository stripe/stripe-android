package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Parcelize
data class AddressSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("billing_details[address]"),
    val allowedCountryCodes: Set<String> = CountryUtils.supportedBillingCountries,
    val hideCountry: Boolean = false,
) : FormItemSpec() {
    fun transform(
        initialValues: Map<IdentifierSpec, String?>,
        shippingValues: Map<IdentifierSpec, String?>?,
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    ): List<FormElement> {
        val sameAsShippingElement =
            shippingValues?.get(IdentifierSpec.SameAsShipping)
                ?.toBooleanStrictOrNull()
                ?.let {
                    SameAsShippingElement(
                        identifier = IdentifierSpec.SameAsShipping,
                        controller = SameAsShippingController(it)
                    )
                }
        val addressElement = autocompleteAddressInteractorFactory?.let {
            AutocompleteAddressElement(
                identifier = apiPath,
                initialValues = initialValues,
                countryCodes = allowedCountryCodes,
                sameAsShippingElement = sameAsShippingElement,
                shippingValuesMap = shippingValues,
                hideCountry = hideCountry,
                interactorFactory = autocompleteAddressInteractorFactory,
            )
        } ?: run {
            AddressElement(
                _identifier = apiPath,
                rawValuesMap = initialValues,
                countryCodes = allowedCountryCodes,
                addressInputMode = AddressInputMode.NoAutocomplete(),
                sameAsShippingElement = sameAsShippingElement,
                shippingValuesMap = shippingValues,
                hideCountry = hideCountry,
            )
        }

        return listOfNotNull(
            createSectionElement(
                sectionFieldElement = addressElement,
                label = resolvableString(R.string.stripe_billing_details)
            ),
            sameAsShippingElement,
        )
    }
}
