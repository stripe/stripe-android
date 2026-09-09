package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.forms.FormFieldEntry

/**
 * The field ID comes from the form element (section, static text, etc).
 */
internal data class FormFieldValues(
    val fieldValuePairs: Map<FormFieldId, FormFieldEntry> = mapOf(),
    val userRequestedReuse: PaymentSelection.CustomerRequestedSave
)
