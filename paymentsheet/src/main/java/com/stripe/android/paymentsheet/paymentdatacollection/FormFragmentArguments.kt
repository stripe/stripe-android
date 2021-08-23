package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.parcelize.Parcelize

@Parcelize
data class FormFragmentArguments(
    val supportedPaymentMethodName: String,
    val saveForFutureUseInitialVisibility: Boolean,
    val saveForFutureUseInitialValue: Boolean,
    val merchantName: String,
    val billingDetails: BillingDetails? = null,
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

fun FormFragmentArguments.getValue(id: IdentifierSpec) =
    when (id) {
        IdentifierSpec.Name -> this.billingDetails?.name
        IdentifierSpec.Email -> this.billingDetails?.email
        IdentifierSpec.Phone -> this.billingDetails?.phone
        IdentifierSpec.Line2 -> this.billingDetails?.address?.line1
        IdentifierSpec.Line1 -> this.billingDetails?.address?.line2
        IdentifierSpec.City -> this.billingDetails?.address?.city
        IdentifierSpec.State -> this.billingDetails?.address?.state
        IdentifierSpec.Country -> this.billingDetails?.address?.country
        IdentifierSpec.PostalCode -> this.billingDetails?.address?.postalCode
        else -> null
    }
