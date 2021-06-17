package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.specification.IdentifierSpec

class FormFieldValues(private val fieldValuePairs: Map<IdentifierSpec, String?> = mapOf()) {
    fun getMap(): Map<IdentifierSpec, String?> = fieldValuePairs
}