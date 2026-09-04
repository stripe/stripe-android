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
 * Uniquely identifies an element in a form. The predefined IDs are used when pre-populating
 * fields and extracting their values.
 * @param ignoreField set this to true to ensure that the field does not get put in the params list
 * when making a Stripe request. Used in [FieldValuesToParamsMapConverter.kt]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class FormFieldId(
    val v1: String,
    val ignoreField: Boolean = false,
    val destination: ParameterDestination = ParameterDestination.Api.Params,
) : Parcelable {
    constructor() : this("")

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun Generic(_value: String) = FormFieldId(_value)

        // Needed to pre-populate forms
        val Name = FormFieldId("billing_details[name]")

        val CardBrand = FormFieldId("card[brand]")

        val PreferredCardBrand = FormFieldId("card[networks][preferred]")

        val CardNumber = FormFieldId("card[number]")

        val CardCvc = FormFieldId("card[cvc]")

        val CardExpMonth = FormFieldId("card[exp_month]")

        val CardExpYear = FormFieldId("card[exp_year]")

        val BillingAddress = FormFieldId("billing_details[address]")

        val Email = FormFieldId("billing_details[email]")

        val Phone = FormFieldId("billing_details[phone]")

        val Line1 = FormFieldId("billing_details[address][line1]")

        val Line2 = FormFieldId("billing_details[address][line2]")

        val City = FormFieldId("billing_details[address][city]")

        // FieldValuesToParamsMapConverter will ignore this in the parameter list
        val DependentLocality = FormFieldId("")

        val PostalCode = FormFieldId("billing_details[address][postal_code]")

        val SortingCode = FormFieldId("")

        val State = FormFieldId("billing_details[address][state]")

        val Country = FormFieldId("billing_details[address][country]")

        // Unique extracting functionality
        val SaveForFutureUse = FormFieldId("save_for_future_use")
        val OneLineAddress = FormFieldId("address")
        val SameAsShipping = FormFieldId("same_as_shipping", ignoreField = true)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val SetAsDefaultPaymentMethod = FormFieldId(
            v1 = "set_as_default_payment_method",
            destination = ParameterDestination.Local.Extras
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val Blik = FormFieldId("blik", destination = ParameterDestination.Api.Options)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val BlikCode = FormFieldId("blik[code]", destination = ParameterDestination.Api.Options)

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val KonbiniConfirmationNumber = FormFieldId(
            v1 = "konbini[confirmation_number]",
            destination = ParameterDestination.Api.Options
        )

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val BacsDebitConfirmed = FormFieldId(
            "bacs_debit[confirmed]",
            destination = ParameterDestination.Local.Extras
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val PhoneNumberCountry = FormFieldId(
            v1 = "phone_number_country",
            destination = ParameterDestination.Local.Extras
        )

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val CardValidatedScan = FormFieldId(
            "card[validated_scan]",
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
