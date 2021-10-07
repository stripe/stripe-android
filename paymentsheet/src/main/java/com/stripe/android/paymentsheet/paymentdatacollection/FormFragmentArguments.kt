package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.payments.core.injection.InjectorKey
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.model.Amount
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class FormFragmentArguments(
    val paymentMethod: SupportedPaymentMethod,
    val showCheckbox: Boolean,
    val showCheckboxControlledFields: Boolean,
    val merchantName: String,
    val amount: Amount? = null,
    val billingDetails: PaymentSheet.BillingDetails? = null,
    @InjectorKey val injectorKey: String,
) : Parcelable

internal fun FormFragmentArguments.getValue(id: IdentifierSpec) =
    when (id) {
        IdentifierSpec.Name -> this.billingDetails?.name
        IdentifierSpec.Email -> this.billingDetails?.email
        IdentifierSpec.Phone -> this.billingDetails?.phone
        IdentifierSpec.Line1 -> this.billingDetails?.address?.line1
        IdentifierSpec.Line2 -> this.billingDetails?.address?.line2
        IdentifierSpec.City -> this.billingDetails?.address?.city
        IdentifierSpec.State -> this.billingDetails?.address?.state
        IdentifierSpec.Country -> this.billingDetails?.address?.country
        IdentifierSpec.PostalCode -> this.billingDetails?.address?.postalCode
        else -> null
    }
