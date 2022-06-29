package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.convertToFormValuesMap
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class FormFragmentArguments(
    val paymentMethodCode: PaymentMethodCode,
    val showCheckbox: Boolean,
    val showCheckboxControlledFields: Boolean,
    val merchantName: String,
    val amount: Amount? = null,
    val billingDetails: PaymentSheet.BillingDetails? = null,
    @InjectorKey val injectorKey: String,
    val initialPaymentMethodCreateParams: PaymentMethodCreateParams? = null
) : Parcelable

internal fun FormFragmentArguments.getInitialValuesMap(): Map<IdentifierSpec, String?> {
    val initialValues = initialPaymentMethodCreateParams?.let {
        convertToFormValuesMap(it.toParamMap())
    } ?: emptyMap()

    return mapOf(
        IdentifierSpec.Name to this.billingDetails?.name,
        IdentifierSpec.Email to this.billingDetails?.email,
        IdentifierSpec.Phone to this.billingDetails?.phone,
        IdentifierSpec.Line1 to this.billingDetails?.address?.line1,
        IdentifierSpec.Line2 to this.billingDetails?.address?.line2,
        IdentifierSpec.City to this.billingDetails?.address?.city,
        IdentifierSpec.State to this.billingDetails?.address?.state,
        IdentifierSpec.Country to this.billingDetails?.address?.country,
        IdentifierSpec.PostalCode to this.billingDetails?.address?.postalCode
    ).plus(initialValues)
}
