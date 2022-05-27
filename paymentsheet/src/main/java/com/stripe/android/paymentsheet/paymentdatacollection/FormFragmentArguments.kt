package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Parcelable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.IdentifierSpec
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

    list.clear()
    initialPaymentMethodCreateParams?.let {
        addPath(it.toParamMap(), "")
    }

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
    ).plus(list.toMap())
}

private val list = mutableListOf<Pair<IdentifierSpec, String?>>()
private fun addPath(map: Map<String, Any?>, path: String) {
    for (entry in map.entries) {
        when (entry.value) {
            null -> {
                list.add(IdentifierSpec.get(addPathKey(path, entry.key)) to null)
            }
            is String -> {
                list.add(IdentifierSpec.get(addPathKey(path, entry.key)) to entry.value as String)
            }
            is Map<*, *> -> {
                addPath(entry.value as Map<String, Any>, addPathKey(path, entry.key))
            }
        }
    }
}

private fun addPathKey(original: String, add: String) = if (original.isEmpty()) {
    add
} else {
    "$original[$add]"
}
