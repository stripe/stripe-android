package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * This uniquely identifies a element in the form.  The vals here are for identifier
 * specs that need to be found when pre-populating fields, or when extracting data.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@kotlinx.serialization.Serializable
@Parcelize
data class IdentifierSpec(val v1: String) : Parcelable {
    constructor() : this("") {
    }

    companion object {
        fun Generic(_value: String) = IdentifierSpec(_value)

        // Needed to pre-populate forms
        val Name = IdentifierSpec("billing_details[name]")

        val CardBrand = IdentifierSpec("card[brand]")

        val CardNumber = IdentifierSpec("card[number]")

        val CardCvc = IdentifierSpec("card[cvc]")

        val Email = IdentifierSpec("billing_details[email]")

        val Phone = IdentifierSpec("billing_details[phone]")

        val Line1 = IdentifierSpec("billing_details[address][line1]")

        val Line2 = IdentifierSpec("billing_details[address][line2]")

        val City = IdentifierSpec("billing_details[address][city]")

        val PostalCode = IdentifierSpec("billing_details[address][postal_code]")

        val State = IdentifierSpec("billing_details[address][state]")

        val Country = IdentifierSpec("billing_details[address][country]")

        // Unique extracting functionality
        val SaveForFutureUse = IdentifierSpec("save_for_future_use")

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        fun get(value: String) = when (value) {
            CardBrand.v1 -> CardBrand
            CardNumber.v1 -> CardNumber
            CardCvc.v1 -> CardCvc
            City.v1 -> City
            Country.v1 -> Country
            Email.v1 -> Email
            Line1.v1 -> Line1
            Line2.v1 -> Line2
            Name.v1 -> Name
            Phone.v1 -> Phone
            PostalCode.v1 -> PostalCode
            SaveForFutureUse.v1 -> SaveForFutureUse
            State.v1 -> State
            else -> {
                Generic(value)
            }
        }
    }
}
