package com.stripe.android.paymentsheet.elements

/**
 * This uniquely identifies a element in the form.  The objects here are for identifier
 * specs that need to be found when pre-populating fields, or when extracting data.
 */
internal sealed class IdentifierSpec(val value: String) {
    data class Generic(private val _value: String) : IdentifierSpec(_value)

    // Needed to pre-populate forms
    object Name : IdentifierSpec("name")
    object Email : IdentifierSpec("email")
    object Phone : IdentifierSpec("phone")
    object Line1 : IdentifierSpec("line1")
    object Line2 : IdentifierSpec("line2")
    object City : IdentifierSpec("city")
    object PostalCode : IdentifierSpec("postal_code")
    object State : IdentifierSpec("state")
    object Country : IdentifierSpec("country")

    // Unique extracting functionality
    object SaveForFutureUse : IdentifierSpec("save_for_future_use")

    object PreFilledParameterMap : IdentifierSpec("save_for_future_use")
}
