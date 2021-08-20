package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.Identifier

/**
 * The identifier here comes from the form element (section, static text, etc)
 */
class FormFieldValues(
    val fieldValuePairs: Map<Identifier, FormFieldEntry> = mapOf(),
    val saveForFutureUse: Boolean,
    val showsMandate: Boolean
)

data class FormFieldEntry(
    val value: String?,
    val isComplete: Boolean
)
