package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.getIdInputControllerMap

/**
 * Takes a list of form elements and populate them with the values in
 * the [FormFieldValues].
 */
internal fun populateWith(
    elements: List<FormElement>,
    formFieldValues: FormFieldValues
) {
    val formFieldValueMap = formFieldValues.fieldValuePairs
    elements.getIdInputControllerMap()
        .forEach { formElementEntry ->
            formFieldValueMap[formElementEntry.key]?.let { input ->
                input.value?.let { inputValue ->
//                    formElementEntry.value.onRawValueChange(inputValue)
                }
            }
        }
}
