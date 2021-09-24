package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.IdentifierSpec

/**
 * The identifier here comes from the form element (section, static text, etc)
 */
internal class FormFieldValues(
    val fieldValuePairs: Map<IdentifierSpec, FormFieldEntry> = mapOf(),
    val showsMandate: Boolean,
    val userRequestedReuse: Boolean
)

internal data class FormFieldEntry(
    val value: String?,
    val isComplete: Boolean = false
)
