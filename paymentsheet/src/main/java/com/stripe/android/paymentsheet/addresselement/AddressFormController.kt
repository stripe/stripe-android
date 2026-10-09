package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.uicore.elements.AddressFieldConfiguration
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.utils.mapAsStateFlow

internal class AddressFormController(
    val initialValues: Map<FormFieldId, String?>,
    val config: AddressLauncher.Configuration?,
    val interactor: AutocompleteAddressInteractor,
) {
    private val autocompleteAddressElement = AutocompleteAddressElement(
        identifier = FormFieldId.Generic("address"),
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

    // Rendered defaults and formatting are part of the baseline, not user edits.
    private val initialFormValues = getCurrentInputValues()

    fun hasChanges(): Boolean = getCurrentInputValues() != initialFormValues

    fun getCurrentFormValues() = autocompleteAddressElement.getFormFieldValueFlow()
        .value
        .filter {
            it.second.isComplete
        }
        .toMap()

    fun setRawValues(values: Map<FormFieldId, String?>) = autocompleteAddressElement.setRawValue(values)

    private fun getCurrentInputValues(): Map<FormFieldId, String> {
        val addressElement = autocompleteAddressElement.sectionFieldErrorController().addressElementFlow.value
        val formValues = addressElement.getFormFieldValueFlow().value.toMap()
        val values = formValues
            .filterKeys { it != FormFieldId.OneLineAddress }
            .mapValues { it.value.value.orEmpty() }
            .toMutableMap()
        // Condensed autocomplete text is not included in submitted form values.
        if (FormFieldId.Line1 !in formValues) {
            values[FormFieldId.Line1] = addressElement.inlineQuery.value
        }
        return values.filterValues { it.isNotEmpty() }
    }
}
