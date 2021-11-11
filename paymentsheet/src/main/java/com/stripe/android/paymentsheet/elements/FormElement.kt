package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow

/**
 * This is used to define each section in the visual form layout.
 * Each item in the layout has an identifier and a controller associated with it.
 */
internal sealed class FormElement {
    abstract val identifier: IdentifierSpec
    abstract val controller: Controller?

    abstract fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>>
}
