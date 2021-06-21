package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec.SectionFieldSpec

class FormFieldValues(val fieldValuePairs: Map<SectionFieldSpec, String?> = mapOf()) {
    fun update(field: SectionFieldSpec, fieldValue: String): FormFieldValues {
        return FormFieldValues(fieldValuePairs.plus(field to fieldValue))
    }
}