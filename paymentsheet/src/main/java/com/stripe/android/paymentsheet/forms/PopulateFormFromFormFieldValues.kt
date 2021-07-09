package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.FormElement

/**
 * This class will take a list of form elements and populate
 * them with the values in the [FormFieldValues]
 */
internal fun populateWith(
    elements: List<FormElement>,
    formFieldValues: FormFieldValues
) {
    val formFieldValueMap = formFieldValues.fieldValuePairs
    elements
        .filter { it.controller != null }
        .forEach { formElement ->
            formFieldValueMap[formElement.identifier]?.let { input ->
                input.value?.let { inputValue ->
                    formElement.controller?.onRawValueChange(inputValue)
                }
            }
        }
}
