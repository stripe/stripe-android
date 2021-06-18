package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.FormElement


internal class PopulateFormFromFormFieldValues(
    val elements: List<FormElement>
) {
    fun populateWith(
        formFieldValues: FormFieldValues
    ) {
        val formFieldValueMap = formFieldValues.fieldValuePairs
        elements
            .filter { it.controller != null }
            .forEach {
                formFieldValueMap[it.identifier]?.let { input ->
                    it.controller?.onValueChange(input)
                }
            }
    }
}