package com.stripe.android.paymentsheet.forms

import android.util.Log

class FormFieldValues(private val fieldValuePairs: Map<Field, String?> = mapOf()) {
    fun update(field: Field, fieldValue: String): FormFieldValues {
        Log.e("STRIPE", "update field value: " + fieldValue)
        return FormFieldValues(fieldValuePairs.plus(field to fieldValue))
    }

    fun getMap(): Map<Field, String?> = fieldValuePairs
}