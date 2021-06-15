package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec.SectionFieldSpec

class FormFieldValues(private val fieldValuePairs: Map<SectionFieldSpec, String?> = mapOf()) {
    fun update(field: SectionFieldSpec, fieldValue: String): FormFieldValues {
        return FormFieldValues(fieldValuePairs.plus(field to fieldValue))
    }

    fun getMap(): Map<SectionFieldSpec, String?> = fieldValuePairs
}