package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.uicore.elements.AddressFieldConfiguration
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.utils.mapAsStateFlow

internal class AddressFormController(
    val initialValues: Map<IdentifierSpec, String?>,
    val config: AddressLauncher.Configuration?,
    val interactor: AutocompleteAddressInteractor,
) {
    private val autocompleteAddressElement = AutocompleteAddressElement(
        identifier = IdentifierSpec.Generic("address"),
        initialValues = initialValues,
        countryCodes = config?.allowedCountries ?: CountryUtils.supportedBillingCountries,
        nameConfig = AddressFieldConfiguration.REQUIRED,
        phoneNumberConfig = parsePhoneNumberConfig(config?.additionalFields?.phone),
        shippingValuesMap = null,
        sameAsShippingElement = null,
        interactorFactory = { interactor },
    )

    val elements: List<FormElement> = listOf(SectionElement.wrap(autocompleteAddressElement))

    val uncompletedFormValues = autocompleteAddressElement.getFormFieldValueFlow().mapAsStateFlow { entries ->
        entries.toMap()
    }

    val completeFormValues = uncompletedFormValues.mapAsStateFlow { entries ->
        entries.takeIf { it.all { entry -> entry.value.isComplete } }
    }

    val lastTextFieldIdentifier = autocompleteAddressElement.getTextFieldIdentifiers()
        .mapAsStateFlow { textFieldControllerIds ->
            textFieldControllerIds.lastOrNull()
        }

    fun getCurrentFormValues() = autocompleteAddressElement.getFormFieldValueFlow()
        .value
        .filter {
            it.second.isComplete
        }
        .toMap()
}
