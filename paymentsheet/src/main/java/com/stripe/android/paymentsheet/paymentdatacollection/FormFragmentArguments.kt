package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.IdentifierSpec
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class FormFragmentArguments(
    val paymentMethod: SupportedPaymentMethod,
    val showCheckbox: Boolean,
    val showCheckboxControlledFields: Boolean,
    val merchantName: String,
    val amount: Amount? = null,
    val billingDetails: PaymentSheet.BillingDetails? = null,
    val requiresUserOptInToSavePaymentMethod: Boolean = false,
    @InjectorKey val injectorKey: String,
) : Parcelable

internal fun FormFragmentArguments.getInitialValuesMap() =
    mapOf(
        IdentifierSpec.Name to this.billingDetails?.name,
        IdentifierSpec.Email to this.billingDetails?.email,
        IdentifierSpec.Phone to this.billingDetails?.phone,
        IdentifierSpec.Line1 to this.billingDetails?.address?.line1,
        IdentifierSpec.Line2 to this.billingDetails?.address?.line2,
        IdentifierSpec.City to this.billingDetails?.address?.city,
        IdentifierSpec.State to this.billingDetails?.address?.state,
        IdentifierSpec.Country to this.billingDetails?.address?.country,
        IdentifierSpec.PostalCode to this.billingDetails?.address?.postalCode
    )
