package com.stripe.android.ui.core.elements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This defines an empty form spec. It is not intended to be used when building forms for
 * PaymentSheet. This form solves an issue where {@link CompleteFormFieldValueFilter#filterFlow()}
 * returns null when filtering no elements. If given this EmptyFormSpec, the filtering will view
 * the form as complete. {@link LayoutSpec#create()} is the way to build a form with no elements.
 */
@Serializable
@SerialName("empty")
internal object EmptyFormSpec : FormItemSpec() {
    override val api_path: IdentifierSpec?
        get() = IdentifierSpec.Generic("empty")
}
