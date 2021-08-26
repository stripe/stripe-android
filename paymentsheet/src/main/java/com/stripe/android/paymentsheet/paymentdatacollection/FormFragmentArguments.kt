package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class FormFragmentArguments(
    val supportedPaymentMethodName: String,
    val saveForFutureUseInitialVisibility: Boolean,
    val saveForFutureUseInitialValue: Boolean,
    val merchantName: String,
    val billingDetails: PaymentSheet.BillingDetails? = null,
) : Parcelable

internal fun FormFragmentArguments.getValue(id: IdentifierSpec) =
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
