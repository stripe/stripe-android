package com.stripe.android.paymentsheet.forms

import android.util.Log

class FormFieldValues(private val fieldValuePairs: Map<SectionSpec.SectionFieldSpec, String?> = mapOf()) {
    fun update(field: SectionSpec.SectionFieldSpec, fieldValue: String): FormFieldValues {
        Log.e("STRIPE", "update field value: " + fieldValue)
        return FormFieldValues(fieldValuePairs.plus(field to fieldValue))
    }

    fun getMap(): Map<SectionSpec.SectionFieldSpec, String?> = fieldValuePairs
}