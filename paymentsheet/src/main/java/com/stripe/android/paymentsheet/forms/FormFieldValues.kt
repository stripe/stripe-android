package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.specifications.IdentifierSpec

/**
 * The identifier here comes from the form element (section, static text, etc)
 */
class FormFieldValues(val fieldValuePairs: Map<IdentifierSpec, String?> = mapOf())
