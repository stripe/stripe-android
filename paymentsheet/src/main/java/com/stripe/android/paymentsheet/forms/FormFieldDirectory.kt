package com.stripe.android.paymentsheet.forms

class FormFieldValues(private val fieldValuePairs: Map<SectionSpec.SectionFieldSpec, String?> = mapOf()) {
    fun update(field: SectionSpec.SectionFieldSpec, fieldValue: String): FormFieldValues {
        return FormFieldValues(fieldValuePairs.plus(field to fieldValue))
    }

    fun getMap(): Map<SectionSpec.SectionFieldSpec, String?> = fieldValuePairs
}