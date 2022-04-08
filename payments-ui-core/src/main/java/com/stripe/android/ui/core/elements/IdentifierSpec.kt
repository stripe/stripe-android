package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * This uniquely identifies a element in the form.  The objects here are for identifier
 * specs that need to be found when pre-populating fields, or when extracting data.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class IdentifierSpec(val value: String) : Parcelable {
    @Parcelize
    data class Generic(private val _value: String) : IdentifierSpec(_value)

    // Needed to pre-populate forms
    @Parcelize
    object Name : IdentifierSpec("billing_details[name]")

    @Parcelize
    object CardBrand : IdentifierSpec("card[brand]")

    @Parcelize
    object CardNumber : IdentifierSpec("card[number]")

    @Parcelize
    object Email : IdentifierSpec("billing_details[email]")

    @Parcelize
    object Phone : IdentifierSpec("billing_details[phone]")

    @Parcelize
    object Line1 : IdentifierSpec("billing_details[address][line1]")

    @Parcelize
    object Line2 : IdentifierSpec("billing_details[address][line2]")

    @Parcelize
    object City : IdentifierSpec("billing_details[address][city]")

    @Parcelize
    object PostalCode : IdentifierSpec("billing_details[address][postal_code]")

    @Parcelize
    object State : IdentifierSpec("billing_details[address][state]")

    @Parcelize
    object Country : IdentifierSpec("billing_details[address][country]")

    // Unique extracting functionality
    @Parcelize
    object SaveForFutureUse : IdentifierSpec("save_for_future_use")
}
