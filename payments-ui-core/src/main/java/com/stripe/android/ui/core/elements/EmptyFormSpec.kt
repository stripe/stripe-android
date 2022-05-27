package com.stripe.android.ui.core.elements

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * This defines an empty form spec. It is not intended to be used when building forms for
 * PaymentSheet. This form solves an issue where {@link CompleteFormFieldValueFilter#filterFlow()}
 * returns null when filtering no elements. If given this EmptyFormSpec, the filtering will view
 * the form as complete. {@link LayoutSpec#create()} is the way to build a form with no elements.
 */
@Serializable
internal object EmptyFormSpec : FormItemSpec() {
    @IgnoredOnParcel
    override val apiPath: IdentifierSpec = IdentifierSpec.Generic("empty")
}
