package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.paymentsheet.Identifier
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.parcelize.Parcelize

@Parcelize
data class ComposeFragmentArguments(
    val supportedPaymentMethodName: String,
    val saveForFutureUseInitialVisibility: Boolean,
    val saveForFutureUseInitialValue: Boolean,
    val merchantName: String,
    val billingDetails: BillingDetails?,
) : Parcelable

@Parcelize
data class Address(
    val city: String? = null,
    val country: String? = null,
    val line1: String? = null,
    val line2: String? = null,
    val postalCode: String? = null,
    val state: String? = null
) : Parcelable

@Parcelize
data class BillingDetails(
    val address: Address?,
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null
) : Parcelable

fun ComposeFragmentArguments.getValue(id: IdentifierSpec) =
    when (id.value) {
        "name" -> this.billingDetails?.name
        "email" -> this.billingDetails?.email
        "phone" -> this.billingDetails?.phone
        "line1" -> this.billingDetails?.address?.line1
        "line2" -> this.billingDetails?.address?.line2
        "city" -> this.billingDetails?.address?.city
        "state" -> this.billingDetails?.address?.state
        "country" -> this.billingDetails?.address?.country
        "postal_code" -> this.billingDetails?.address?.postalCode
        else -> null
    }

fun ComposeFragmentArguments.getValue(id: Identifier) =
    when (id) {
        Identifier.Name -> this.billingDetails?.name
        Identifier.Email -> this.billingDetails?.email
        Identifier.Phone -> this.billingDetails?.phone
        Identifier.Line1 -> this.billingDetails?.address?.line1
        Identifier.Line2 -> this.billingDetails?.address?.line2
        Identifier.City -> this.billingDetails?.address?.city
        Identifier.State -> this.billingDetails?.address?.state
        Identifier.Country -> this.billingDetails?.address?.country
        Identifier.PostalCode -> this.billingDetails?.address?.postalCode
        Identifier.SaveForFutureUse -> this.saveForFutureUseInitialValue.toString()
        is Identifier.Generic -> null
    }
