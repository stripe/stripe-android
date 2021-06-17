package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.common.FormElement

internal class PopulateFormFromFormFieldValues(
    val elements: List<FormElement>
) {
    // TODO: This can be very much simplified.
    // This maps the field type to the controller
    private val idControllerMap = elements
        .filter { it.controller != null }
        .associate { Pair(it.identifier, it.controller!!) }

    fun populateWith(
        formFieldValues: FormFieldValues
    ) {
        val formFieldValueMap = formFieldValues.getMap()
        idControllerMap.entries.forEach { entry ->
            elements.filter { it. }
            formFieldValueMap[entry.key]?.let {
                entry.value.onValueChange(it)
            }
        }
    }
}