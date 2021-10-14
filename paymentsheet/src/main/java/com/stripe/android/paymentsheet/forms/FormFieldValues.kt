package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.model.PaymentSelection

/**
 * The identifier here comes from the form element (section, static text, etc)
 */
internal class FormFieldValues(
    val fieldValuePairs: Map<IdentifierSpec, FormFieldEntry> = mapOf(),
    val showsMandate: Boolean,
    val userRequestedReuse: PaymentSelection.UserReuseRequest
)

internal data class FormFieldEntry(
    val value: String?,
    val isComplete: Boolean = false
)
