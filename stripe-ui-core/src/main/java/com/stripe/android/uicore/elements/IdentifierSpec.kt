package com.stripe.android.uicore.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface ParameterDestination : Parcelable {
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Api : ParameterDestination {
        Params,
        Options
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Local : ParameterDestination {
        Extras
    }
}

/**
 * This uniquely identifies a element in the form.  The vals here are for identifier
 * specs that need to be found when pre-populating fields, or when extracting data.
 * @param ignoreField set this to true to ensure that the field does not get put in the params list
 * when making a Stripe request. Used in [FieldValuesToParamsMapConverter.kt]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class IdentifierSpec(
    val v1: String,
    val ignoreField: Boolean = false,
    val destination: ParameterDestination = ParameterDestination.Api.Params,
) : Parcelable {
    constructor() : this("")

    companion object {
        fun Generic(_value: String) = IdentifierSpec(_value)

        // Needed to pre-populate forms
        val Name = IdentifierSpec("billing_details[name]")

        val CardBrand = IdentifierSpec("card[brand]")

        val PreferredCardBrand = IdentifierSpec("card[networks][preferred]")

        val CardNumber = IdentifierSpec("card[number]")

        val CardCvc = IdentifierSpec("card[cvc]")

        val CardExpMonth = IdentifierSpec("card[exp_month]")

        val CardExpYear = IdentifierSpec("card[exp_year]")

        val BillingAddress = IdentifierSpec("billing_details[address]")

        val Email = IdentifierSpec("billing_details[email]")

        val Phone = IdentifierSpec("billing_details[phone]")

        val Line1 = IdentifierSpec("billing_details[address][line1]")

        val Line2 = IdentifierSpec("billing_details[address][line2]")

        val City = IdentifierSpec("billing_details[address][city]")

        // FieldValuesToParamsMapConverter will ignore this in the parameter list
        val DependentLocality = IdentifierSpec("")

        val PostalCode = IdentifierSpec("billing_details[address][postal_code]")

        val SortingCode = IdentifierSpec("")

        val State = IdentifierSpec("billing_details[address][state]")

        val Country = IdentifierSpec("billing_details[address][country]")

        // Unique extracting functionality
        val SaveForFutureUse = IdentifierSpec("save_for_future_use")
        val OneLineAddress = IdentifierSpec("address")
        val SameAsShipping = IdentifierSpec("same_as_shipping", ignoreField = true)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val SetAsDefaultPaymentMethod = IdentifierSpec("set_as_default_payment_method")

        val Upi = IdentifierSpec("upi")
        val Vpa = IdentifierSpec("upi[vpa]")

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val Blik = IdentifierSpec("blik", destination = ParameterDestination.Api.Options)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val BlikCode = IdentifierSpec("blik[code]", destination = ParameterDestination.Api.Options)

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val KonbiniConfirmationNumber = IdentifierSpec(
            v1 = "konbini[confirmation_number]",
            destination = ParameterDestination.Api.Options
        )

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val BacsDebitConfirmed = IdentifierSpec(
            "bacs_debit[confirmed]",
            destination = ParameterDestination.Local.Extras
        )

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
            OneLineAddress.v1 -> OneLineAddress
            else -> {
                Generic(value)
            }
        }
    }
}
