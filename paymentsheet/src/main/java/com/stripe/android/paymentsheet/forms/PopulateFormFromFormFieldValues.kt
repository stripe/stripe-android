package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.FormElement
import com.stripe.android.paymentsheet.idInputControllerMap

/**
 * This class will take a list of form elements and populate
 * them with the values in the [FormFieldValues]
 */
internal class PopulateFormFromFormFieldValues(
    val elements: List<FormElement>
) {
    fun populateWith(
        formFieldValues: FormFieldValues
    ) {
        val formFieldValueMap = formFieldValues.fieldValuePairs
        elements.idInputControllerMap()
            .forEach { formElementEntry ->
                formFieldValueMap[formElementEntry.key]?.let { input ->
                    input.value?.let { inputValue ->
                        formElementEntry.value.onRawValueChange(inputValue)
                    }
                }
            }
    }
}
