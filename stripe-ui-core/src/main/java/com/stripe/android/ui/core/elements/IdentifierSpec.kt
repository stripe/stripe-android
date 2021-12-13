package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * This uniquely identifies a element in the form.  The objects here are for identifier
 * specs that need to be found when pre-populating fields, or when extracting data.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class IdentifierSpec(val value: String) : Parcelable{
    @Parcelize
    data class Generic(private val _value: String) : IdentifierSpec(_value)

    // Needed to pre-populate forms
    @Parcelize
    object Name : IdentifierSpec("name")
    @Parcelize
    object Email : IdentifierSpec("email")
    @Parcelize
    object Phone : IdentifierSpec("phone")
    @Parcelize
    object Line1 : IdentifierSpec("line1")
    @Parcelize
    object Line2 : IdentifierSpec("line2")
    @Parcelize
    object City : IdentifierSpec("city")
    @Parcelize
    object PostalCode : IdentifierSpec("postal_code")
    @Parcelize
    object State : IdentifierSpec("state")
    @Parcelize
    object Country : IdentifierSpec("country")

    // Unique extracting functionality
    @Parcelize
    object SaveForFutureUse : IdentifierSpec("save_for_future_use")
}
